package com.sweak.qralarm.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.sweak.qralarm.R
import com.sweak.qralarm.core.designsystem.icon.QRAlarmIcons
import com.sweak.qralarm.core.designsystem.theme.QRAlarmTheme
import com.sweak.qralarm.core.designsystem.theme.isQRAlarmTheme
import com.sweak.qralarm.core.designsystem.theme.space
import com.sweak.qralarm.core.ui.model.Code

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteCodeBottomSheet(
    code: Code,
    onConfirmClicked: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = modalBottomSheetState
    ) {
        Column(
            modifier = Modifier
                .padding(
                    start = MaterialTheme.space.mediumLarge,
                    end = MaterialTheme.space.mediumLarge,
                    bottom = MaterialTheme.space.xLarge
                )
        ) {
            Text(
                text = stringResource(R.string.delete_this_code),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(bottom = MaterialTheme.space.medium)
            )

            Text(
                text = stringResource(R.string.delete_this_code_description),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = MaterialTheme.space.mediumLarge)
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (MaterialTheme.isQRAlarmTheme) Color.White
                    else Color.Unspecified,
                    contentColor = if (MaterialTheme.isQRAlarmTheme) Color.Black
                    else Color.Unspecified
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.space.medium)
                ) {
                    Icon(
                        imageVector = QRAlarmIcons.QrCode,
                        contentDescription =
                            stringResource(R.string.content_description_qr_code_icon),
                        modifier = Modifier.padding(end = MaterialTheme.space.medium)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        if (code.name != null) {
                            Text(
                                text = code.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = code.value,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = code.value,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.space.mediumLarge)
            ) {
                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = with(MaterialTheme) {
                            if (isQRAlarmTheme) colorScheme.onSurface else Color.Unspecified
                        }
                    )
                }

                Spacer(modifier = Modifier.width(MaterialTheme.space.medium))

                Button(
                    onClick = onConfirmClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.delete))
                }
            }
        }
    }
}

@Preview
@Composable
private fun DeleteCodeBottomSheetPreview() {
    QRAlarmTheme {
        DeleteCodeBottomSheet(
            code = Code(
                id = 1,
                value = "1324678965131687461",
                name = "Coffee bag code"
            ),
            onConfirmClicked = {},
            onDismissRequest = {}
        )
    }
}
