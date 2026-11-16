/*
Copyright 2024 Jigsaw Operations LLC / YuhaDNS
Licensed under the Apache License, Version 2.0
*/
package app.intra.ui;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.widget.RemoteViews;

import app.intra.R;
import app.intra.sys.VpnController;
import app.intra.sys.VpnState;

/**
 * AppWidget for displaying real-time YuhaDNS status, today's blocked ad counter,
 * and one-click toggle functionality directly from the Home Screen.
 */
public class YuhaDnsWidget extends AppWidgetProvider {

  public static final String ACTION_TOGGLE_VPN = "com.yuha.dns.widget.ACTION_TOGGLE_VPN";

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    for (int appWidgetId : appWidgetIds) {
      updateAppWidget(context, appWidgetManager, appWidgetId);
    }
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    super.onReceive(context, intent);
    if (ACTION_TOGGLE_VPN.equals(intent.getAction())) {
      VpnState vpnState = VpnController.getInstance().getState(context);
      if (vpnState.activationRequested) {
        VpnController.getInstance().stop(context);
      } else {
        if (VpnService.prepare(context) == null) {
          VpnController.getInstance().start(context);
        } else {
          Intent mainIntent = new Intent(context, MainActivity.class);
          mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          context.startActivity(mainIntent);
        }
      }
      updateAllWidgets(context);
    }
  }

  public static void updateAllWidgets(Context context) {
    AppWidgetManager manager = AppWidgetManager.getInstance(context);
    if (manager != null) {
      int[] ids = manager.getAppWidgetIds(new ComponentName(context, YuhaDnsWidget.class));
      for (int id : ids) {
        updateAppWidget(context, manager, id);
      }
    }
  }

  static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
    VpnState vpnState = VpnController.getInstance().getState(context);
    long blockedCount = VpnController.getInstance().getTracker(context).getNumBlockedToday();

    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_yuha_dns);

    boolean isActive = vpnState.activationRequested;
    views.setTextViewText(R.id.widget_status, isActive ? "🛡️ YuhaDNS 보호 중" : "⚠️ YuhaDNS 일시 중지됨");
    views.setTextColor(R.id.widget_status, isActive ? 0xFF388E3C : 0xFF757575);
    views.setTextViewText(R.id.widget_blocked, "오늘 차단한 광고: " + blockedCount + "개");
    views.setTextViewText(R.id.widget_button, isActive ? "끄기" : "켜기");

    // Click on widget background -> Open MainActivity
    Intent openAppIntent = new Intent(context, MainActivity.class);
    PendingIntent openAppPendingIntent = PendingIntent.getActivity(
        context, 0, openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
    );
    views.setOnClickPendingIntent(R.id.widget_container, openAppPendingIntent);

    // Click on toggle button -> Toggle VPN
    Intent toggleIntent = new Intent(context, YuhaDnsWidget.class);
    toggleIntent.setAction(ACTION_TOGGLE_VPN);
    PendingIntent togglePendingIntent = PendingIntent.getBroadcast(
        context, 1, toggleIntent,
        PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
    );
    views.setOnClickPendingIntent(R.id.widget_button, togglePendingIntent);

    appWidgetManager.updateAppWidget(appWidgetId, views);
  }
}
