package com.example.kasisave

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

class MilestoneWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val intent = Intent(context, MilestoneWidgetService::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)) // Required for uniqueness

            val views = RemoteViews(context.packageName, R.layout.milestone_widget)
            views.setRemoteAdapter(R.id.milestone_list, intent)
            views.setEmptyView(R.id.milestone_list, android.R.id.empty)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}


