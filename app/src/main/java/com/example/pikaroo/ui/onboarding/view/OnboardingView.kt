package com.example.pikaroo.ui.onboarding.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pikaroo.ui.onboarding.viewmodel.OnboardingViewModel
import com.example.pikaroo.ui.theme.OnboardingAmberButton
import com.example.pikaroo.ui.theme.OnboardingAmberEnd
import com.example.pikaroo.ui.theme.OnboardingAmberStart
import com.example.pikaroo.ui.theme.OnboardingDescription
import com.example.pikaroo.ui.theme.OnboardingGreenButton
import com.example.pikaroo.ui.theme.OnboardingGreenEnd
import com.example.pikaroo.ui.theme.OnboardingGreenStart
import com.example.pikaroo.ui.theme.OnboardingOrangeEnd
import com.example.pikaroo.ui.theme.OnboardingOrangeStart

@Composable
fun OnboardingView(
    onFinishOnboarding: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val pagerState = rememberPagerState(
        pageCount = { uiState.pages.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChanged(pagerState.currentPage)
    }

    LaunchedEffect(uiState.currentPageIndex) {
        if (pagerState.currentPage != uiState.currentPageIndex) {
            pagerState.animateScrollToPage(uiState.currentPageIndex)
        }
    }

    val finishOnboarding: () -> Unit = {
        viewModel.completeOnboarding()
        onFinishOnboarding()
    }

    val currentPage = uiState.pages.getOrNull(
        uiState.currentPageIndex
    ) ?: return

    val accentColor = when (uiState.currentPageIndex) {
        0 -> OnboardingOrangeStart
        1 -> OnboardingAmberButton
        else -> OnboardingGreenButton
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .safeDrawingPadding()
    ) {
        // Parte superior: ilustración, fondo e indicadores.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.56f)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val page = uiState.pages[pageIndex]

                val backgroundColors = when (pageIndex) {
                    0 -> listOf(
                        OnboardingOrangeStart,
                        OnboardingOrangeEnd
                    )

                    1 -> listOf(
                        OnboardingAmberStart,
                        OnboardingAmberEnd
                    )

                    else -> listOf(
                        OnboardingGreenStart,
                        OnboardingGreenEnd
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(backgroundColors)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(page.imageRes),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth(0.78f)
                            .fillMaxHeight(0.72f)
                    )
                }
            }

            if (!uiState.isLastPage) {
                TextButton(
                    onClick = finishOnboarding,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 16.dp)
                ) {
                    Text(
                        text = "Saltar",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                uiState.pages.forEachIndexed { index, _ ->
                    val selected = index == uiState.currentPageIndex

                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (selected) 28.dp else 8.dp)
                            .background(
                                color = if (selected) {
                                    Color.White
                                } else {
                                    Color.White.copy(alpha = 0.45f)
                                },
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
            }
        }

        // Parte inferior: textos y botones.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.44f)
                .padding(horizontal = 30.dp)
                .padding(top = 24.dp, bottom = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                val pageNumber = (uiState.currentPageIndex + 1)
                    .toString()
                    .padStart(2, '0')

                val totalPages = uiState.pages.size
                    .toString()
                    .padStart(2, '0')

                Text(
                    text = "$pageNumber / $totalPages",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = currentPage.title,
                    color = Color.Black,
                    fontSize = 38.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = currentPage.description,
                    color = OnboardingDescription,
                    fontSize = 16.sp,
                    lineHeight = 25.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!uiState.isLastPage) {
                    TextButton(
                        onClick = finishOnboarding
                    ) {
                        Text(
                            text = "Saltar",
                            color = OnboardingDescription,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        if (uiState.isLastPage) {
                            finishOnboarding()
                        } else {
                            viewModel.goToNextPage()
                        }
                    },
                    modifier = Modifier.heightIn(min = 52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(
                        horizontal = 24.dp,
                        vertical = 12.dp
                    )
                ) {
                    Text(
                        text = if (uiState.isLastPage) {
                            "Comenzar"
                        } else {
                            "Siguiente"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "→",
                        fontSize = 22.sp
                    )
                }
            }
        }
    }
}