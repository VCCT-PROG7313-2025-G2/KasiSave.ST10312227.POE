package com.example.kasisave

import android.content.Intent
import android.widget.RemoteViewsService

class MilestoneWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return MilestoneRemoteViewsFactory(applicationContext)
    }
}
