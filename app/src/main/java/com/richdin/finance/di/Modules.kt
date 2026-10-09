package com.richdin.finance.di

import android.content.Context
import com.richdin.finance.core.database.*
import com.richdin.finance.core.repository.FinanceRepository
import com.richdin.finance.core.repository.FinanceRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RichdinDatabase {
        return RichdinDatabase.buildDatabase(context)
    }

    @Provides
    fun provideSalaryPeriodDao(db: RichdinDatabase): SalaryPeriodDao = db.salaryPeriodDao()

    @Provides
    fun provideIncomeSourceDao(db: RichdinDatabase): IncomeSourceDao = db.incomeSourceDao()

    @Provides
    fun providePocketDao(db: RichdinDatabase): PocketDao = db.pocketDao()

    @Provides
    fun provideExpenseDao(db: RichdinDatabase): ExpenseDao = db.expenseDao()

    @Provides
    fun provideLoanDao(db: RichdinDatabase): LoanDao = db.loanDao()

    @Provides
    fun provideLoanLimitConfigDao(db: RichdinDatabase): LoanLimitConfigDao = db.loanLimitConfigDao()

    @Provides
    fun provideCategoryDao(db: RichdinDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideGoalDao(db: RichdinDatabase): GoalDao = db.goalDao()

    @Provides
    fun provideBackupMetaDao(db: RichdinDatabase): BackupMetaDao = db.backupMetaDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFinanceRepository(
        impl: FinanceRepositoryImpl
    ): FinanceRepository
}
