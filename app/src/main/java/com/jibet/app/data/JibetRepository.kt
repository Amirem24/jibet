package com.jibet.app.data

import com.jibet.app.util.JDate
import com.jibet.app.util.PRESET_ACCOUNTS
import com.jibet.app.util.PRESET_CATEGORIES
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.min
import kotlin.math.roundToInt

/** خلاصه‌ی خانه */
data class Summary(
    val todayIncome: Double,
    val todayExpense: Double,
    val monthIncome: Double,
    val monthExpense: Double,
)

/** داده‌ی گزارش */
data class ReportBucket(val key: String, val label: String, val expense: Double, val income: Double)

data class ReportCategory(
    val categoryId: Long,
    val name: String,
    val color: String,
    val icon: String,
    val total: Double,
    val pct: Int,
)

data class ReportData(
    val period: String,
    val start: Long,
    val end: Long,
    val totalExpense: Double,
    val totalIncome: Double,
    val byCategory: List<ReportCategory>,
    val trend: List<ReportBucket>,
)

/** نتیجه‌ی صفحه‌بندی تراکنش‌ها — offset همراه نتیجه حمل می‌شود تا رخدادهای کهنه
 *  (مثلاً هنگام تغییر هم‌زمان فیلتر) اشتباهی به‌جای صفحه‌ی اول نمایش داده نشوند */
data class TxPage(val items: List<TxRow>, val total: Int, val income: Double, val expense: Double, val offset: Int = 0)

/** پیش‌نویس حساب برای آنبوردینگ */
data class AccountDraft(val key: String, val balance: Double)

class JibetRepository(
    internal val db: JibetDatabase,
    private val scope: CoroutineScope,
) {
    internal val dao = db.dao()

    /* ================= جریان‌های پایه ================= */

    fun watchAccounts(): Flow<List<AccountRow>> = dao.watchAccounts()
    fun watchCategories(): Flow<List<CategoryEntity>> = dao.watchCategories()
    fun watchGoals(): Flow<List<GoalEntity>> = dao.watchGoals()
    fun watchRecurrings(): Flow<List<RecurringRow>> = dao.watchRecurrings()
    fun watchTotalBalance(): Flow<Double> = dao.watchTotalBalance()

    /** خلاصه‌ی امروز + این ماه شمسی — بازه‌ها هر دقیقه (و بعد از نیمه‌شب) بازمحاسبه می‌شوند
     *  تا اگر اپ باز بماند و روز/ماه عوض شود، اعداد کهنه نشوند */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun watchSummary(): Flow<Summary> = dayTick().flatMapLatest {
        val today = LocalDate.now()
        val (mStart, mEnd, _) = JDate.jalaliMonthRange(today)
        combine(
            dao.watchMoneyInRange(JDate.startOfDay(today), JDate.endOfDay(today)),
            dao.watchMoneyInRange(mStart, mEnd),
        ) { t, m -> Summary(t.income, t.expense, m.income, m.expense) }
    }

    /** همه‌ی داده‌های لازم برای شیت جزئیات */
    fun watchTx(id: Long): Flow<TxRow?> = dao.watchTx(id)

    /* ================= تراکنش‌ها ================= */

    /** لیست صفحه‌بندی‌شده با فیلترها — واکنش‌گرا */
    fun watchTxPage(
        q: String,
        kind: Kind?,
        accountId: Long,
        categoryId: Long,
        from: Long,
        to: Long,
        limit: Int,
        offset: Int,
    ): Flow<TxPage> {
        // جستجوی مبلغ: فقط وقتی عبارت جستجو رقم دارد، روی عدد (بدون جداکننده) هم LIKE می‌زنیم
        val qNum = com.jibet.app.util.Fmt.digitsOnly(q)
        // FIX (debug): % و _ در متن جستجو قبلاً wildcard SQL بودند («%» همه‌ی تراکنش‌ها را برمی‌گرداند)؛
        // حالا literal جستجو می‌شوند (سمت DAO هم ESCAPE '\\' اضافه شد)
        val qEscaped = q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return combine(
            dao.watchTxPage(
                q = qEscaped,
                qNum = qNum,
                kindIsAll = kind == null,
                kind = kind ?: Kind.EXPENSE,
                accountId = accountId,
                categoryId = categoryId,
                from = from,
                to = to,
                limit = limit,
                offset = offset,
            ),
            dao.watchTxTotals(
                q = qEscaped,
                qNum = qNum,
                kindIsAll = kind == null,
                kind = kind ?: Kind.EXPENSE,
                accountId = accountId,
                categoryId = categoryId,
                from = from,
                to = to,
            ),
        ) { items, totals -> TxPage(items, totals.total, totals.income, totals.expense, offset) }
    }

    /* ================= گزارش ================= */

    /** گزارش دوره‌ای — واکنش‌گرا نسبت به تغییر تراکنش‌ها؛ بازه‌ها هم مثل خلاصه تازه می‌مانند */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun watchReport(period: String, refMillis: Long, kind: Kind): Flow<ReportData> =
        dayTick().flatMapLatest {
            val ref = JDate.localDate(refMillis)
            val (start, end, bucketsSeed) = when (period) {
                "week" -> {
                    val (s, e) = JDate.persianWeekRange(ref)
                    Triple(s, e, weekBuckets(JDate.localDate(s)))
                }
                "year" -> {
                    val (s, e, j) = JDate.jalaliYearRange(ref)
                    Triple(s, e, yearBuckets(j.jy))
                }
                else -> {
                    val (s, e, j) = JDate.jalaliMonthRange(ref)
                    Triple(s, e, monthBuckets(j.jy, j.jm))
                }
            }
            dao.watchTxInRange(start, end).map { txs ->
                buildReport(period, start, end, kind, bucketsSeed, txs)
            }
        }

    /** تیک بازمحاسبه‌ی بازه‌های زمانی: هر دقیقه، و ۱ ثانیه بعد از نیمه‌شب
     *  (برای پاس‌دادن روز/ماه بدون اینکه اپ باید دوباره باز شود) */
    private fun dayTick(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            val nowZ = LocalDateTime.now()
            val untilMidnight =
                Duration.between(nowZ, nowZ.toLocalDate().plusDays(1).atStartOfDay()).toMillis() + 1_000
            delay(untilMidnight.coerceIn(1_000L, 60_000L))
        }
    }

    private fun weekBuckets(start: LocalDate): List<ReportBucket> {
        val names = com.jibet.app.util.JDate.J_WEEKDAYS
        return (0..6).map { i ->
            val d = start.plusDays(i.toLong())
            ReportBucket(d.toString(), names[i], 0.0, 0.0)
        }
    }

    private fun yearBuckets(jy: Int): List<ReportBucket> =
        (1..12).map { m -> ReportBucket("$jy-$m", JDate.J_MONTHS[m - 1], 0.0, 0.0) }

    private fun monthBuckets(jy: Int, jm: Int): List<ReportBucket> {
        val len = JDate.jalaliMonthLength(jy, jm)
        return (1..len).map { d ->
            ReportBucket("$jy-$jm-$d", com.jibet.app.util.Fmt.faDigits(d), 0.0, 0.0)
        }
    }

    private fun buildReport(
        period: String,
        start: Long,
        end: Long,
        kind: Kind,
        seed: List<ReportBucket>,
        txs: List<TxRow>,
    ): ReportData {
        val trend = seed.toMutableList()
        val trendIndex = trend.withIndex().associate { (i, b) -> b.key to i }
        var totalExpense = 0.0
        var totalIncome = 0.0
        val catMap = LinkedHashMap<Long, ReportCategory>(0)
        for (t in txs) {
            if (t.kind == Kind.INCOME) totalIncome += t.amount else totalExpense += t.amount
            val bucketKey = when (period) {
                "week" -> JDate.localDate(t.date).toString()
                "year" -> {
                    val j = JDate.toJalali(t.date)
                    "${j.jy}-${j.jm}"
                }
                else -> {
                    val j = JDate.toJalali(t.date)
                    "${j.jy}-${j.jm}-${j.jd}"
                }
            }
            val idx = trendIndex[bucketKey]
            if (idx != null) {
                val b = trend[idx]
                trend[idx] = if (t.kind == Kind.INCOME) b.copy(income = b.income + t.amount)
                else b.copy(expense = b.expense + t.amount)
            }
            if (t.kind == kind) {
                val cur = catMap[t.categoryId]
                if (cur == null) {
                    catMap[t.categoryId] = ReportCategory(
                        categoryId = t.categoryId,
                        name = t.catName ?: "بدون دسته",
                        color = t.catColor ?: "#8A93A6",
                        icon = t.catIcon ?: "more-horizontal",
                        total = t.amount,
                        pct = 0,
                    )
                } else {
                    catMap[t.categoryId] = cur.copy(total = cur.total + t.amount)
                }
            }
        }
        val total = if (kind == Kind.INCOME) totalIncome else totalExpense
        val byCategory = catMap.values
            .map { it.copy(pct = if (total > 0) (it.total / total * 100.0).roundToInt() else 0) }
            .sortedByDescending { it.total }
        return ReportData(period, start, end, totalExpense, totalIncome, byCategory, trend)
    }

    /* ================= بودجه‌ها ================= */

    fun watchBudgets(jy: Int, jm: Int): Flow<List<BudgetRow>> {
        val start = JDate.startOfDay(JDate.toGregorian(jy, jm, 1))
        val end = JDate.endOfDay(
            JDate.toGregorian(jy, jm, JDate.jalaliMonthLength(jy, jm))
        )
        return dao.watchBudgets(jy, jm, start, end)
    }

    /* ================= نوشتن ================= */

    suspend fun addAccount(name: String, kind: AccountKind, initialBalance: Double, colorHex: String, icon: String): Long =
        dao.insertAccount(
            AccountEntity(
                name = name.trim(),
                kind = kind,
                initialBalance = initialBalance,
                colorHex = colorHex,
                icon = icon,
                sortOrder = dao.allAccounts().size,
            )
        )

    suspend fun updateAccount(a: AccountEntity) = dao.updateAccount(a)

    /** حذف حساب — فقط بدون تراکنش و بدون قاعده‌ی تکراری؛ اتمیک */
    suspend fun deleteAccount(a: AccountEntity) {
        db.withTransactionCompat {
            if (dao.countAccountTx(a.id) > 0) {
                throw IllegalStateException("این حساب تراکنش دارد؛ ابتدا تراکنش‌هایش را حذف کنید")
            }
            if (dao.countAccountRecurring(a.id) > 0) {
                throw IllegalStateException("این حساب در تراکنش‌های تکراری استفاده شده؛ ابتدا آن قواعد را حذف یا ویرایش کنید")
            }
            dao.deleteAccount(a)
        }
    }

    suspend fun addCategory(name: String, kind: Kind, colorHex: String, icon: String): Long =
        dao.insertCategory(
            CategoryEntity(
                name = name.trim(),
                kind = kind,
                colorHex = colorHex,
                icon = icon,
                sortOrder = dao.allCategories().size,
            )
        )

    suspend fun updateCategory(c: CategoryEntity) = dao.updateCategory(c)

    /** حذف دسته — فقط بدون تراکنش و بدون قاعده‌ی تکراری؛ بودجه‌های همین دسته هم پاک می‌شوند؛ اتمیک */
    suspend fun deleteCategory(c: CategoryEntity) {
        db.withTransactionCompat {
            if (dao.countCategoryTx(c.id) > 0) {
                throw IllegalStateException("این دسته تراکنش دارد و قابل حذف نیست")
            }
            if (dao.countCategoryRecurring(c.id) > 0) {
                throw IllegalStateException("این دسته در تراکنش‌های تکراری استفاده شده؛ ابتدا آن قواعد را حذف یا ویرایش کنید")
            }
            dao.deleteBudgetsForCategory(c.id)
            dao.deleteCategory(c)
        }
    }

    suspend fun addTx(
        amount: Double,
        kind: Kind,
        title: String,
        note: String?,
        date: Long,
        accountId: Long,
        categoryId: Long,
        recurringId: Long? = null,
    ): Long = dao.insertTx(
        TransactionEntity(
            amount = amount,
            kind = kind,
            title = title.trim(),
            note = note?.trim()?.ifEmpty { null },
            date = date,
            accountId = accountId,
            categoryId = categoryId,
            recurringId = recurringId,
        )
    )

    suspend fun updateTx(t: TransactionEntity) = dao.updateTx(t)

    suspend fun deleteTx(t: TransactionEntity) = dao.deleteTx(t)

    suspend fun addBudget(categoryId: Long, jYear: Int, jMonth: Int, amount: Double): Long {
        val existing = dao.findBudget(categoryId, jYear, jMonth)
        return dao.insertBudget(
            (existing ?: BudgetEntity(categoryId = categoryId, jYear = jYear, jMonth = jMonth, amount = 0.0))
                .copy(amount = amount)
        )
    }

    suspend fun deleteBudget(b: BudgetEntity) = dao.deleteBudget(b)

    suspend fun addGoal(title: String, targetAmount: Double, savedAmount: Double, deadline: String?, colorHex: String, icon: String): Long =
        dao.insertGoal(
            GoalEntity(
                title = title.trim(),
                targetAmount = targetAmount,
                savedAmount = savedAmount,
                deadline = deadline,
                colorHex = colorHex,
                icon = icon,
            )
        )

    suspend fun updateGoal(g: GoalEntity) = dao.updateGoal(g)

    // FIX (debug): خواندن+نوشتن داخل تراکنش تا دو واریز هم‌زمان یکی دیگری را نبلعد
    suspend fun depositGoal(id: Long, delta: Double) = db.withTransactionCompat {
        val g = dao.getGoal(id) ?: return@withTransactionCompat
        dao.updateGoal(g.copy(savedAmount = g.savedAmount + delta))
    }

    suspend fun deleteGoal(g: GoalEntity) = dao.deleteGoal(g)

    suspend fun addRecurring(
        title: String,
        amount: Double,
        kind: Kind,
        accountId: Long,
        categoryId: Long,
        frequency: Frequency,
        nextDate: Long,
    ): Long = dao.insertRecurring(
        RecurringEntity(
            title = title.trim(),
            amount = amount,
            kind = kind,
            accountId = accountId,
            categoryId = categoryId,
            frequency = frequency,
            nextDate = nextDate,
            // لنگر روز ماه از همان اول ذخیره می‌شود تا پیشروی ماهانه دریفت نکند
            anchorDay = java.time.Instant.ofEpochMilli(nextDate).atZone(ZoneId.systemDefault()).dayOfMonth,
        )
    )

    suspend fun updateRecurring(r: RecurringEntity) = dao.updateRecurring(r)

    suspend fun toggleRecurring(r: RecurringEntity) =
        dao.updateRecurring(r.copy(isActive = !r.isActive))

    suspend fun deleteRecurring(r: RecurringEntity) = dao.deleteRecurring(r)

    /* ================= آنبوردینگ ================= */

    /** ذخیره‌ی انتخاب‌های ویزارد؛ فقط وقتی خالی است اجرا می‌شود */
    // FIX (debug): کل ستاپ داخل یک تراکنش است تا قطع‌شدن پروسه وسط کار، نصف حساب/دسته نسازد
    suspend fun setup(accounts: List<AccountDraft>, categoryKeys: List<String>) = db.withTransactionCompat {
        val accCount = dao.countAccounts()
        val catCount = dao.countCategories()
        if (accCount == 0) {
            for (draft in accounts) {
                val preset = PRESET_ACCOUNTS.firstOrNull { it.key == draft.key } ?: continue
                dao.insertAccount(
                    AccountEntity(
                        name = preset.name,
                        kind = AccountKind.from(preset.kind),
                        initialBalance = draft.balance,
                        colorHex = preset.color,
                        icon = preset.icon,
                        isDefault = preset.key == "bank",
                        sortOrder = PRESET_ACCOUNTS.indexOf(preset),
                    )
                )
            }
        }
        if (catCount == 0) {
            var order = 0
            for (key in categoryKeys) {
                val preset = PRESET_CATEGORIES.firstOrNull { it.key == key } ?: continue
                dao.insertCategory(
                    CategoryEntity(
                        name = preset.name,
                        kind = Kind.from(preset.kind),
                        colorHex = preset.color,
                        icon = preset.icon,
                        isDefault = true,
                        sortOrder = order,
                    )
                )
                order++
            }
        }
    }

    /* ================= داده‌ی نمونه ================= */

    /** ساخت داده‌ی نمونه؛ فقط وقتی هیچ تراکنشی نیست. خروجی: پیام خطا یا null */
    suspend fun loadSampleData(): String? {
        if (dao.countTx() > 0) return "از قبل داده وجود دارد"
        val accounts = dao.allAccounts()
        val categories = dao.allCategories()
        if (accounts.isEmpty() || categories.isEmpty()) return "ابتدا حساب و دسته بسازید"

        fun expCat(key: String) =
            categories.firstOrNull { it.kind == Kind.EXPENSE && (it.name.contains(key) || it.icon == key) }
        fun incCat(key: String) =
            categories.firstOrNull { it.kind == Kind.INCOME && (it.name.contains(key) || it.icon == key) }

        val cash = accounts[0]
        val card = accounts.getOrNull(1) ?: cash

        data class Tpl(val name: String, val cat: String, val min: Long, val max: Long)
        val templates = listOf(
            Tpl("ناهار رستوران", "خوراک", 80_000, 350_000),
            Tpl("قهوه", "خوراک", 45_000, 120_000),
            Tpl("سوپرمارکت", "خوراک", 150_000, 900_000),
            Tpl("تاکسی اینترنتی", "حمل", 35_000, 180_000),
            Tpl("بنزین", "حمل", 100_000, 400_000),
            Tpl("خرید آنلاین", "خرید", 200_000, 1_500_000),
            Tpl("سینما", "تفریح", 150_000, 500_000),
            Tpl("قبض برق", "قبض", 120_000, 450_000),
            Tpl("قبض اینترنت", "قبض", 250_000, 600_000),
            Tpl("دارو", "سلامت", 90_000, 700_000),
        )

        val now = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val txs = mutableListOf<TransactionEntity>()
        for (dayAgo in 35 downTo 0) {
            val d = now.minusDays(dayAgo.toLong())
            val count = 1 + (0..2).random()
            repeat(count) {
                val t = templates.random()
                val cat = expCat(t.cat) ?: return@repeat
                val amount = ((t.min..t.max).random() / 1000) * 1000.0
                val hour = 9 + (0..11).random()
                val date = java.time.LocalDateTime.of(d.year, d.monthValue, d.dayOfMonth, hour, (0..59).random())
                    .atZone(zone).toInstant().toEpochMilli()
                txs.add(
                    TransactionEntity(
                        amount = amount, kind = Kind.EXPENSE, title = t.name, date = date,
                        accountId = if ((0..1).random() == 1) card.id else cash.id,
                        categoryId = cat.id,
                    )
                )
            }
            val j = JDate.toJalali(d)
            if (j.jd == 1) {
                incCat("حقوق")?.let { salary ->
                    txs.add(
                        TransactionEntity(
                            amount = 28_500_000.0, kind = Kind.INCOME, title = "حقوق ماهانه",
                            date = java.time.LocalDateTime.of(d.year, d.monthValue, d.dayOfMonth, 10, 0)
                                .atZone(zone).toInstant().toEpochMilli(),
                            accountId = card.id, categoryId = salary.id,
                        )
                    )
                }
            }
            if ((0..99).random() < 6) {
                incCat("فروش")?.let { sale ->
                    txs.add(
                        TransactionEntity(
                            amount = ((500_000..3_500_000).random() / 1000) * 1000.0,
                            kind = Kind.INCOME, title = "فروش اقلام قدیمی",
                            date = java.time.LocalDateTime.of(d.year, d.monthValue, d.dayOfMonth, 15, 0)
                                .atZone(zone).toInstant().toEpochMilli(),
                            accountId = cash.id, categoryId = sale.id,
                        )
                    )
                }
            }
        }
        dao.insertTxs(txs)

        // بودجه‌ی ماه جاری
        val (jy, jm) = JDate.toJalali(now).let { it.jy to it.jm }
        val budgetPlan = listOf(
            "خوراک" to 6_000_000.0, "حمل" to 3_000_000.0, "خرید" to 4_000_000.0,
            "تفریح" to 2_000_000.0, "قبض" to 2_500_000.0,
        )
        for ((catKey, amount) in budgetPlan) {
            val cat = expCat(catKey) ?: continue
            if (dao.findBudget(cat.id, jy, jm) == null) {
                dao.insertBudget(BudgetEntity(categoryId = cat.id, amount = amount, jYear = jy, jMonth = jm))
            }
        }

        // هدف‌ها
        if (dao.countGoals() == 0) {
            dao.insertGoal(
                GoalEntity(
                    title = "سفر به ژاپن", targetAmount = 10_000_000.0, savedAmount = 6_000_000.0,
                    deadline = "${jy + 1}/01/15", colorHex = "#5B7FFF", icon = "plane",
                )
            )
            dao.insertGoal(
                GoalEntity(
                    title = "خرید لپ‌تاپ", targetAmount = 40_000_000.0, savedAmount = 5_200_000.0,
                    deadline = "${jy + 1}/12/20", colorHex = "#B07BFF", icon = "laptop",
                )
            )
        }

        // تراکنش‌های تکراری
        if (dao.countRecurringAll() == 0) {
            val rent = expCat("مسکن")
            val internet = expCat("قبض")
            val salary = incCat("حقوق")
            val nextMonth = System.currentTimeMillis() + 30L * 24 * 3600 * 1000
            val nextMonthAnchor = java.time.Instant.ofEpochMilli(nextMonth)
                .atZone(ZoneId.systemDefault()).dayOfMonth
            rent?.let {
                dao.insertRecurring(
                    RecurringEntity(
                        title = "اجاره خانه", amount = 9_000_000.0, kind = Kind.EXPENSE,
                        accountId = card.id, categoryId = it.id, frequency = Frequency.MONTHLY, nextDate = nextMonth,
                        anchorDay = nextMonthAnchor,
                    )
                )
            }
            internet?.let {
                dao.insertRecurring(
                    RecurringEntity(
                        title = "اشتراک اینترنت", amount = 380_000.0, kind = Kind.EXPENSE,
                        accountId = card.id, categoryId = it.id, frequency = Frequency.MONTHLY, nextDate = nextMonth,
                        anchorDay = nextMonthAnchor,
                    )
                )
            }
            salary?.let {
                dao.insertRecurring(
                    RecurringEntity(
                        title = "حقوق ماهانه", amount = 28_500_000.0, kind = Kind.INCOME,
                        accountId = card.id, categoryId = it.id, frequency = Frequency.MONTHLY, nextDate = nextMonth,
                        anchorDay = nextMonthAnchor,
                    )
                )
            }
        }
        return null
    }

    /** پاک‌سازی همه‌ی داده‌ها بدون حذف حساب‌ها و دسته‌ها */
    suspend fun clearData() {
        dao.deleteAllTx()
        dao.deleteAllBudgets()
        dao.deleteAllRecurring()
        dao.deleteAllGoals()
    }

    /* ================= پردازش تراکنش‌های تکراری ================= */

    /** ثبت تراکنش‌های سررسیدشده و پیش‌بردن سررسید بعدی
     *  در شروع برنامه + هر ۱۵ دقیقه (نگاه JibetApp) اجرا می‌شود؛
     *  کل پردازش داخل یک تراکنش است تا اگر پروسه وسط کار کشته شود،
     *  هیچ تراکنشی دوباره ثبت نشود (nextDate و insertها هم‌سرنوشت‌اند). */
    suspend fun processDueRecurring() = db.withTransactionCompat {
        val now = System.currentTimeMillis()
        for (r in dao.activeRecurrings()) {
            var next = r.nextDate
            var anchor = r.anchorDay
            var guard = 0
            // FIX (debug): سقف از ۲۰۰ به ۱۰۰۰ رسید — تکراری روزانه بعد از ~۷ ماه آفلاین‌بودن
            // قبلاً در هر اجرا فقط ۲۰۰ قسط ثبت می‌شد و بقیه ۱۵ دقیقه‌ای قطره‌چکانی می‌آمد
            while (next <= now && guard < 1000) {
                dao.insertTx(
                    TransactionEntity(
                        amount = r.amount,
                        kind = r.kind,
                        title = r.title,
                        date = next,
                        accountId = r.accountId,
                        categoryId = r.categoryId,
                        recurringId = r.id,
                    )
                )
                next = advance(next, r.frequency, anchor)
                guard++
            }
            // برای ردیف‌های قدیمی (anchorDay=0) لنگر از سررسید فعلی استخراج و ذخیره
            // می‌شود تا دریفت انباشته از این به بعد متوقف شود
            if (guard > 0 || anchor !in 1..31) {
                if (anchor !in 1..31) {
                    anchor = java.time.Instant.ofEpochMilli(r.nextDate)
                        .atZone(ZoneId.systemDefault()).dayOfMonth
                }
                dao.updateRecurring(r.copy(nextDate = next, anchorDay = anchor))
            }
        }
    }

    /** پیش‌بردن سررسید — برای ماهانه/سالانه از «لنگر روز ماه» استفاده می‌شود تا
     *  زنجیره‌ی ۳۱ ژانویه ← ۲۸ فوریه ← ۲۸ مارس (دریفت) رخ ندهد */
    private fun advance(millis: Long, f: Frequency, anchorDay: Int): Long {
        val z = ZoneId.systemDefault()
        val ldt = java.time.Instant.ofEpochMilli(millis).atZone(z)
        val anchor = if (anchorDay in 1..31) anchorDay else ldt.dayOfMonth
        val time = ldt.toLocalTime()
        val nextDate = when (f) {
            Frequency.DAILY -> ldt.toLocalDate().plusDays(1)
            Frequency.WEEKLY -> ldt.toLocalDate().plusDays(7)
            Frequency.MONTHLY -> {
                val ym = YearMonth.from(ldt).plusMonths(1)
                ym.atDay(min(anchor, ym.lengthOfMonth()))
            }
            Frequency.YEARLY -> {
                val ym = YearMonth.from(ldt).plusYears(1)
                ym.atDay(min(anchor, ym.lengthOfMonth()))
            }
        }
        return nextDate.atTime(time).atZone(z).toInstant().toEpochMilli()
    }
}
