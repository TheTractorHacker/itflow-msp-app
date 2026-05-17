package com.foleyit.itflow.widget

import android.content.Context
import android.content.Intent
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.color.ColorProviders
import com.foleyit.itflow.MainActivity

class TicketCountWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("widget_cache", Context.MODE_PRIVATE)
        val open    = prefs.getInt("open", 0)
        val overdue = prefs.getInt("overdue", 0)

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
                        .padding(12.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        "ITFlow MSP",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.primary
                        )
                    )
                    Spacer(GlanceModifier.height(8.dp))
                    Row {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text("Open", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                            Text(
                                open.toString(),
                                style = TextStyle(
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlanceTheme.colors.onSurface
                                )
                            )
                        }
                        if (overdue > 0) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text("Overdue", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                                Text(
                                    overdue.toString(),
                                    style = TextStyle(
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorProvider(
                                            android.graphics.Color.parseColor("#D32F2F"),
                                            android.graphics.Color.parseColor("#EF9A9A")
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
