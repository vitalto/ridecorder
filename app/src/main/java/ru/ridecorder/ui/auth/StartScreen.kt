package ru.ridecorder.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ridecorder.R

@Composable
fun StartScreen(onJoinClick: () -> Unit, onLoginClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Фон с изображением
        Image(
            painter = painterResource(id = R.drawable.start_background_image),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Текст и кнопки
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(46.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Текст приложения
            Text(
                text = stringResource(id = R.string.app_name),
                fontSize = 40.sp,
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp)
            )

            // Кнопки
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onJoinClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(text = stringResource(id = R.string.start_register), color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp)
                }
                TextButton(
                    onClick = onLoginClick,
                    modifier = Modifier.fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(text = stringResource(id = R.string.start_login), color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp)
                }
            }
        }
    }
}
