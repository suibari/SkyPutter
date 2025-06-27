package com.suibari.skyputter

import android.app.Application
import com.suibari.skyputter.ui.notification.NotificationViewModel
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.*
import com.suibari.skyputter.data.repository.MainRepository
import com.suibari.skyputter.data.repository.NotificationRepoProvider
import com.suibari.skyputter.data.repository.UserPostRepository
import com.suibari.skyputter.ui.about.AboutScreen
import com.suibari.skyputter.ui.draft.DraftScreen
import com.suibari.skyputter.ui.loading.LoadingScreen
import com.suibari.skyputter.ui.login.LoginScreen
import com.suibari.skyputter.ui.main.MainScreen
import com.suibari.skyputter.ui.main.MainViewModel
import com.suibari.skyputter.ui.notification.NotificationListScreen
import com.suibari.skyputter.ui.post.UserPostListScreen
import com.suibari.skyputter.ui.post.UserPostViewModel
import com.suibari.skyputter.ui.settings.SettingsScreen
import com.suibari.skyputter.ui.theme.SkyPutterTheme
import com.suibari.skyputter.util.DraftViewModel
import com.suibari.skyputter.util.SessionManager
import com.suibari.skyputter.worker.DeviceNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    // セッション情報を保持
    private var sessionState by mutableStateOf<SessionState>(SessionState.Loading)

    // ViewModelContainer を Activity レベルで管理
    private lateinit var viewModelContainer: ViewModelContainer

    sealed class SessionState {
        object Loading : SessionState()
        data class Loaded(val hasSession: Boolean, val myDid: String?) : SessionState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "onCreate: start")

        // ViewModelContainer を初期化
        viewModelContainer = ViewModelContainer(applicationContext)

        // まずUIを設定（軽量）
        setContent {
            SkyPutterTheme {
                AppContent()
            }
        }

        // 重い初期化処理は別スレッドで実行
        initializeApp()
    }

    private fun initializeApp() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("MainActivity", "initializeApp: start")

                // SessionManager初期化
//                SessionManager.initialize(applicationContext)

                // セッション取得（重い処理の可能性があるため別スレッド）
                val session = SessionManager.getSession()
                val hasSession = session.accessJwt != null &&
                        session.refreshJwt != null &&
                        session.did != null
                val myDid = session.did

                Log.d("MainActivity", "initializeApp: session loaded, hasSession=$hasSession")

                // セッションがある場合はViewModelも事前に初期化
                if (hasSession) {
                    Log.d("MainActivity", "Pre-initializing ViewModels for existing session")
                    viewModelContainer.initializeViewModels()
                }

                // メインスレッドでUI状態を更新
                withContext(Dispatchers.Main) {
                    sessionState = SessionState.Loaded(hasSession, myDid)
                }

                Log.d("MainActivity", "initializeApp: completed")

            } catch (e: Exception) {
                Log.e("MainActivity", "initializeApp: error", e)
                // エラー時もUIを更新
                withContext(Dispatchers.Main) {
                    sessionState = SessionState.Loaded(false, null)
                }
            }
        }
    }

    @Composable
    private fun AppContent() {
        val context = LocalContext.current

        when (val state = sessionState) {
            is SessionState.Loading -> {
                // セッション読み込み中の画面
                LoadingScreen()
            }
            is SessionState.Loaded -> {
                AppNavigation(
                    context = context,
                    hasSession = state.hasSession,
                    myDid = state.myDid
                )
            }
        }
    }

    @Composable
    private fun AppNavigation(
        context: Context,
        hasSession: Boolean,
        myDid: String?
    ) {
        val navController = rememberNavController()

        // 下書きテキスト状態
        var selectedDraftText by remember { mutableStateOf("") }

        // 初期化状態を監視
        val initState = viewModelContainer.initializationState

        NavHost(
            navController = navController,
            startDestination = if (hasSession) Screen.Main.route else Screen.Login.route
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    application = application as SkyPutterApp,
                    onLoginSuccess = {
                        navController.navigate(Screen.Loading.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Loading.route) {
                LoadingScreen()

                LaunchedEffect(Unit) {
                    try {
                        Log.d("MainActivity", "Loading screen: starting ViewModel initialization")

                        // ViewModelの初期化を非同期で実行
                        lifecycleScope.launch(Dispatchers.IO) {
                            viewModelContainer.initializeViewModels()

                            delay(300) // UI準備時間

                            withContext(Dispatchers.Main) {
                                Log.d("MainActivity", "Loading screen: navigating to main")
                                navController.navigate(Screen.Main.route) {
                                    popUpTo(Screen.Loading.route) { inclusive = true }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Loading failed", e)
                        // エラー時はログイン画面に戻る
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Loading.route) { inclusive = true }
                        }
                    }
                }
            }

            composable(Screen.Main.route) {
                Log.d("MainActivity", "Main screen composable, initState: $initState")
                // 全く同一のインスタンスなのに、composableが変更されたと認識され再生成される...
                // 何故か分からないが、実害がないのでいったん放置
//                Log.d("MainActivity", "Main screen - ViewModelContainer: $viewModelContainer")
//                Log.d("MainActivity", "Main screen - MainViewModel: ${viewModelContainer.mainViewModel}")

                // ViewModelが初期化完了している場合のみ表示
                when (initState) {
                    is ViewModelContainer.InitializationState.Completed -> {
                        viewModelContainer.mainViewModel?.let { mainVM ->
                            Log.d("MainActivity", "Showing MainScreen")
                            MainScreen(
                                application = application as SkyPutterApp,
                                viewModel = mainVM,
                                notificationViewModel = viewModelContainer.notificationViewModel!!,
                                draftViewModel = viewModelContainer.draftViewModel!!,
                                initialText = selectedDraftText,
                                onLogout = {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        SessionManager.clearSession()
                                        viewModelContainer.notificationViewModel?.stopBackgroundPolling()
                                        withContext(Dispatchers.Main) {
                                            navController.navigate(Screen.Login.route) {
                                                popUpTo(Screen.Main.route) { inclusive = true }
                                            }
                                        }
                                    }
                                },
                                onOpenNotification = {
                                    navController.navigate(Screen.NotificationList.route)
                                },
                                onOpenUserPost = {
                                    navController.navigate(Screen.UserPost.route)
                                },
                                onOpenDraft = {
                                    navController.navigate(Screen.Draft.route)
                                },
                                onOpenSettings = {
                                    navController.navigate(Screen.Settings.route)
                                },
                                onOpenAbout = {
                                    navController.navigate(Screen.About.route)
                                },
                                onDraftTextCleared = {
                                    // 下書きテキストをクリア
                                    selectedDraftText = ""
                                },
                            )
                        } ?: run {
                            Log.d("MainActivity", "MainViewModel is null, showing loading")
                            LoadingScreen()
                        }
                    }
                    is ViewModelContainer.InitializationState.Error -> {
                        Log.e("MainActivity", "ViewModel initialization error: ${initState.exception}")
                        // エラー画面を表示するか、ログイン画面に戻る
                        LaunchedEffect(Unit) {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Main.route) { inclusive = true }
                            }
                        }
                        LoadingScreen()
                    }
                    else -> {
                        Log.d("MainActivity", "ViewModels not ready, showing loading. State: $initState")
                        LoadingScreen()
                    }
                }
            }

            composable(Screen.NotificationList.route) {
                when (initState) {
                    is ViewModelContainer.InitializationState.Completed -> {
                        val notificationVM = viewModelContainer.notificationViewModel
                        val mainVM = viewModelContainer.mainViewModel

                        if (notificationVM != null && mainVM != null) {
                            NotificationListScreen(
                                viewModel = notificationVM,
                                mainViewModel = mainVM,
                                onNavigateToMain = {
                                    navController.navigate("main") {
                                        Log.d("Nav", "Navigating to Main")
                                        navController.popBackStack()
                                    }
                                }
                            )
                        } else {
                            LoadingScreen()
                        }
                    }
                    else -> LoadingScreen()
                }
            }

            composable(Screen.UserPost.route) {
                when (initState) {
                    is ViewModelContainer.InitializationState.Completed -> {
                        viewModelContainer.userPostViewModel?.let { userPostVM ->
                            UserPostListScreen(
                                viewModel = userPostVM,
                                mainViewModel = viewModelContainer.mainViewModel!!,
                                myDid = myDid!!,
                                onNavigateToMain = {
                                    navController.navigate("main") {
                                        Log.d("Nav", "Navigating to Main")
                                        navController.popBackStack()
                                    }
                                },
                            )
                        } ?: LoadingScreen()
                    }
                    else -> LoadingScreen()
                }
            }

//            composable(Screen.LikesBack.route) {
//                when (initState) {
//                    is ViewModelContainer.InitializationState.Completed -> {
//                        val likesBackVM = viewModelContainer.likesBackViewModel
//                        val mainVM = viewModelContainer.mainViewModel
//
//                        if (likesBackVM != null && mainVM != null) {
//                            LikesBackScreen(
//                                viewModel = likesBackVM,
//                                mainViewModel = mainVM,
//                                myDid = myDid!!,
//                                onNavigateToMain = {
//                                    navController.navigate("main")
//                                }
//                            )
//                        } else {
//                            LoadingScreen()
//                        }
//                    }
//                    else -> LoadingScreen()
//                }
//            }

            composable(Screen.Draft.route) {
                when (initState) {
                    is ViewModelContainer.InitializationState.Completed -> {
                        val draftVM = viewModelContainer.draftViewModel

                        if (draftVM != null) {
                            DraftScreen(
                                draftViewModel = draftVM,
                                onBack = {
                                    navController.navigate("main") {
                                        popUpTo("main") { inclusive = true } // 既存のMainを削除してから遷移
                                    }
                                },
                                onDraftSelected = { text, draftId ->
                                    selectedDraftText = text // 先に更新

                                    lifecycleScope.launch(Dispatchers.IO) {
                                        try {
                                            draftVM.deleteDraft(draftId)
                                        } catch (e: Exception) {
                                            Log.e("MainActivity", "Failed to delete draft", e)
                                        }

                                        withContext(Dispatchers.Main) {
                                            Log.d("Nav", "Navigating to Main")
                                            // ここでpopBackStack() すると2重呼び出しで画面が真っ白になる
                                            navController.navigate("main")
                                        }
                                    }
                                }
                            )
                        }
                    }
                    else -> LoadingScreen()
                }
            }

            composable(Screen.Settings.route) {
                when (initState) {
                    is ViewModelContainer.InitializationState.Completed -> {
                        SettingsScreen(
                            onBack = {
                                navController.navigate("main") {
                                    popUpTo("main") { inclusive = true } // 既存のMainを削除してから遷移
                                }
                            }
                        )
                    }
                    else -> LoadingScreen()
                }
            }

            composable(Screen.About.route) {
                when (initState) {
                    is ViewModelContainer.InitializationState.Completed -> {
                        AboutScreen()
                    }
                    else -> LoadingScreen()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val postUri = intent?.getStringExtra("postUri")
        if (postUri != null) {
            Log.d("MainActivity", "Received postUri from notification: $postUri")

            // 通知画面にナビゲート & URI を渡す
            lifecycleScope.launch(Dispatchers.Main) {
                viewModelContainer.notificationViewModel?.targetUri = postUri

                // 通知画面へのナビゲーション要求を emit（SharedFlow 用）
                viewModelContainer.mainViewModel?.navigateToNotification?.emit(Unit)
            }
        }
    }
}

// ViewModelを管理するクラス
class ViewModelContainer(private val context: Context) {

    // 初期化状態を管理
    private var _initializationState by mutableStateOf<InitializationState>(InitializationState.NotStarted)
    val initializationState: InitializationState get() = _initializationState

    // nullable なViewModelプロパティ
    var notificationViewModel: NotificationViewModel? by mutableStateOf(null)
        private set
    var userPostViewModel: UserPostViewModel? by mutableStateOf(null)
        private set
    var draftViewModel: DraftViewModel? by mutableStateOf(null)
        private set
    var mainViewModel: MainViewModel? by mutableStateOf(null)
        private set

    sealed class InitializationState {
        object NotStarted : InitializationState()
        object InProgress : InitializationState()
        object Completed : InitializationState()
        data class Error(val exception: Exception) : InitializationState()
    }

    suspend fun initializeViewModels() {
        // 既に完了している場合はスキップ
        if (_initializationState is InitializationState.Completed &&
            mainViewModel != null &&
            notificationViewModel != null) {
            Log.d("ViewModelContainer", "ViewModels already initialized, skipping")
            return
        }

        // 進行中の場合は待機
        if (_initializationState is InitializationState.InProgress) {
            Log.d("ViewModelContainer", "ViewModel initialization already in progress")
            return
        }

        _initializationState = InitializationState.InProgress
        Log.d("ViewModelContainer", "initializeViewModels: start")

        try {
            withContext(Dispatchers.IO) {
                // Repository の初期化
                Log.d("ViewModelContainer", "Initializing repositories...")
                val deviceNotifier = DeviceNotifier(context)
                val notificationRepo = NotificationRepoProvider.getInstance(context)
                val userPostRepo = UserPostRepository()
                val mainRepo = MainRepository()

                Log.d("ViewModelContainer", "Repositories initialized")

                // ViewModelの作成（メインスレッドで作成）
                withContext(Dispatchers.Main) {
                    Log.d("ViewModelContainer", "Creating ViewModels...")
                    val application = context.applicationContext as Application

                    notificationViewModel = NotificationViewModel(
                        repo = notificationRepo,
                        notifier = deviceNotifier
                    )

                    userPostViewModel = UserPostViewModel(userPostRepo)
                    draftViewModel = DraftViewModel(context)

                    mainViewModel = MainViewModel(
                        repo = mainRepo,
                        userPostViewModel = userPostViewModel!!,
                        notificationViewModel = notificationViewModel!!,
                    )

                    // 初期化処理
                    mainViewModel?.initialize(context)

                    Log.d("ViewModelContainer", "ViewModels created successfully")
                }
            }

            _initializationState = InitializationState.Completed
            Log.d("ViewModelContainer", "initializeViewModels: completed successfully")

        } catch (e: Exception) {
            Log.e("ViewModelContainer", "initializeViewModels: error", e)
            _initializationState = InitializationState.Error(e)
            throw e
        }
    }
}
