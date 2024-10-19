package com.gfms.koinoor.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gfms.koinoor.R
import com.gfms.koinoor.ui.components.KoinoorUIDivider
import com.gfms.koinoor.ui.theme.AlphaNearOpaque
import com.gfms.koinoor.ui.theme.KoinoorTheme
import com.google.accompanist.insets.statusBarsPadding

@Composable
fun KoinoorUIDestinationBar(modifier: Modifier = Modifier) {
    Column(modifier = modifier.statusBarsPadding()) {
        TopAppBar(
            backgroundColor = KoinoorTheme.colors.uiBackground.copy(alpha = AlphaNearOpaque),
            contentColor = KoinoorTheme.colors.textSecondary,
            elevation = 0.dp
        ) {
            Text(
                text = "Delivery to 1600 Amphitheater Way",
                style = MaterialTheme.typography.subtitle1,
                color = KoinoorTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            )
            IconButton(
                onClick = { /* todo */ },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    tint = KoinoorTheme.colors.brand,
                    contentDescription = stringResource(R.string.label_select_delivery)
                )
            }
        }
        KoinoorUIDivider()
    }
}

@Preview("default")
@Preview("dark theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview("large font", fontScale = 2f)
@Composable
fun PreviewDestinationBar() {
    KoinoorTheme() {
        KoinoorUIDestinationBar()
    }
}