package sk.kasper.space

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ProcessLifecycleOwner
import com.crashlytics.android.Crashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.picasso.Picasso
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.fabric.sdk.android.Fabric
import sk.kasper.space.analytics.Analytics
import sk.kasper.space.analytics.FirebaseAnalyticsLogger
import sk.kasper.space.api.RemoteApi
import sk.kasper.space.database.Database
import sk.kasper.space.di.AppComponent
import sk.kasper.space.di.AppModule
import sk.kasper.space.di.DaggerAppComponent
import sk.kasper.space.notification.showLaunchNotificationJob.LaunchNotificationChecker
import sk.kasper.space.settings.SettingKey
import sk.kasper.space.settings.SettingsManager
import sk.kasper.space.sync.SyncJobService
import timber.log.Timber
import javax.inject.Inject

// todo rozchod CI na githube
open class SpaceApp: Application(), HasAndroidInjector {

    @Inject
    lateinit var checker: LaunchNotificationChecker

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var remoteApi: RemoteApi

    @Inject
    lateinit var database: Database

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun androidInjector(): AndroidInjector<Any> = dispatchingAndroidInjector

    protected open fun createAppComponent(): AppComponent {
        return DaggerAppComponent.builder()
                .application(this)
                .appModule(AppModule(this))
                .build()
    }

    override fun onCreate() {
        super.onCreate()

        AndroidThreeTen.init(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(false)

            Picasso.setSingletonInstance(Picasso.Builder(this)
                    .listener { picasso, uri, exception ->
                        Timber.e(exception, "uri: $uri")
                    }
                    .indicatorsEnabled(true)
                    .build())
        } else {
            Fabric.with(this, Crashlytics())
            Timber.plant(CrashReportingTree())
            Analytics.plant(FirebaseAnalyticsLogger(this))
        }

        createAppComponent().inject(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(checker)

        SyncJobService.startPeriodicLaunchesSyncJob(this)

        AppCompatDelegate.setDefaultNightMode(settingsManager.nightMode)

        settingsManager.addSettingChangeListener { settingKey ->
            if (settingKey == SettingKey.NIGHT_MODE || settingKey == SettingKey.NIGHT_MODE_PRE_Q) {
                AppCompatDelegate.setDefaultNightMode(settingsManager.nightMode)
            }
        }
    }
}

class CrashReportingTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.WARN || priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO) {
            return
        }

        Crashlytics.setInt("priority", priority)
        Crashlytics.setString("tag", tag)
        Crashlytics.setString("message", message)

        if (t == null) {
            Crashlytics.logException(Exception(message))
        } else {
            Crashlytics.logException(t)
        }
    }

}
