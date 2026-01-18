package com.example.machacksobjecttracjer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.machacksobjecttracjer.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// --- Data Models ---
data class ApiResponse(val status: String, val data: List<SightingData>?)
data class SightingData(val image_url: String, val timestamp: String)

// --- API Interface ---
interface TrackerApi {
    @GET("api/locate/{item}/")
    suspend fun locateItem(@Path("item") item: String): ApiResponse
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(TrackerApi::class.java)

        setContent {
            AppTheme {
                TrackerApp(api)
            }
        }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val appColors = lightColorScheme(
        primary = Color(0xFFFF7828),
        onPrimary = Color.White,
        background = Color.White,
        onBackground = Color.Black,
        surface = Color.White,
        onSurface = Color.Black
    )

    MaterialTheme(
        colorScheme = appColors,
        typography = MaterialTheme.typography,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerApp(api: TrackerApi) {
    val navController = rememberNavController()
    Scaffold {
        AppNavHost(navController = navController, api = api, modifier = Modifier.padding(it))
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    api: TrackerApi,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable("result/{itemName}") { backStackEntry ->
            val itemName = backStackEntry.arguments?.getString("itemName") ?: ""
            ResultScreen(itemName = itemName, api = api, navController = navController)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.2f))
            Image(
                painter = painterResource(id = R.drawable.neue_machina_1),
                contentDescription = "Logo",
                modifier = Modifier.padding(bottom = 72.dp)
            )

            // Buttons in a grid-like structure
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)
                ) {
                    ItemButton(text = "Keys", icon = Icons.Default.Key, onClick = { navController.navigate("result/keys") })
                    ItemButton(text = "Glasses", icon = Icons.Default.Search, onClick = { navController.navigate("result/glasses") })
                }
                ItemButton(text = "Phone", icon = Icons.Default.Phone, onClick = { navController.navigate("result/phone") })
            }
            Spacer(Modifier.weight(0.3f))
        }
    }
}


@Composable
fun ItemButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.size(140.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(icon, contentDescription = text, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(itemName: String, api: TrackerApi, navController: NavController) {
    var sightings by remember { mutableStateOf<List<SightingData>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(itemName) {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val response = api.locateItem(itemName)
                if (response.status == "found" && !response.data.isNullOrEmpty()) {
                    sightings = response.data
                } else {
                    errorMessage = "Could not find $itemName."
                }
            } catch (e: Exception) {
                errorMessage = "Error: Could not connect to the server. Please check the IP address and network connection."
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Big, centered, orange back arrow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Searching...", style = MaterialTheme.typography.bodyLarge)
                } else {
                    ResultView(sightings = sightings, errorMessage = errorMessage)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ResultView(sightings: List<SightingData>, errorMessage: String?) {
    if (sightings.isNotEmpty()) {
        val pagerState = rememberPagerState { sightings.size }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val sighting = sightings[page]
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        AsyncImage(
                            model = BuildConfig.BASE_URL.trimEnd('/') + sighting.image_url,
                            contentDescription = "Located Item",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Page indicator and timestamp
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "Image ${pagerState.currentPage + 1} of ${pagerState.pageCount}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                val currentSighting = sightings.getOrNull(pagerState.currentPage)
                if (currentSighting != null) {
                    val formattedTimestamp = OffsetDateTime.parse(currentSighting.timestamp).minusHours(5)
                        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
                    Text(
                        text = formattedTimestamp,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    } else if (errorMessage != null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
