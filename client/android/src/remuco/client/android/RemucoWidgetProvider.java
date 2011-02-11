/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2010 by the Remuco team, see AUTHORS.
 *
 *   This file is part of Remuco.
 *
 *   Remuco is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Remuco is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Remuco.  If not, see <http://www.gnu.org/licenses/>.
 *   
 */

package remuco.client.android;

import remuco.client.common.util.Log;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class RemucoWidgetProvider extends AppWidgetProvider {

    public static String ACTION_PREV = "prev";
    public static String ACTION_PLAY = "play";
    public static String ACTION_NEXT = "next";

	// --- the player adapter
	protected PlayerAdapter player = null;

    protected static int[] appWidgetIds;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        if (player == null) {
            createPlayer(context);
        }

        this.appWidgetIds = appWidgetIds;

        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];

            // Get the layout for the App Widget and attach an on-click listener to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.remucowidget);

            Intent intentRemuco = new Intent(context, Remuco.class);
            PendingIntent pendingIntentRemuco = PendingIntent.getActivity(context, 0, intentRemuco, 0);
            views.setOnClickPendingIntent(R.id.WidgetBackground, pendingIntentRemuco);

            Intent intentPrev = new Intent(context, RemucoWidgetProvider.class);
            intentPrev.setAction(ACTION_PREV);
            PendingIntent pendingIntentPrev = PendingIntent.getBroadcast(context, 0, intentPrev, 0);
            views.setOnClickPendingIntent(R.id.WidgetPrev, pendingIntentPrev);

            Intent intentPlay = new Intent(context, RemucoWidgetProvider.class);
            intentPlay.setAction(ACTION_PLAY);
            PendingIntent pendingIntentPlay = PendingIntent.getBroadcast(context, 0, intentPlay, 0);
            views.setOnClickPendingIntent(R.id.WidgetPlay, pendingIntentPlay);

            Intent intentNext = new Intent(context, RemucoWidgetProvider.class);
            intentNext.setAction(ACTION_NEXT);
            PendingIntent pendingIntentNext = PendingIntent.getBroadcast(context, 0, intentNext, 0);
            views.setOnClickPendingIntent(R.id.WidgetNext, pendingIntentNext);

            // Tell the AppWidgetManager to perform an update on the current App Widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (player == null) {
            createPlayer(context);
        }

        if (intent.getAction().equals(ACTION_PREV)) {
            player.getPlayer().ctrlPrev();
        } else if (intent.getAction().equals(ACTION_PLAY)) {
            player.getPlayer().ctrlPlayPause();
        } else if (intent.getAction().equals(ACTION_NEXT)) {
            player.getPlayer().ctrlNext();
        } else {
            super.onReceive(context, intent);
        }
    }

    private void createPlayer(Context context) {
        player = RemucoActivity.connect(context, 140);

        // --- create view handler
        WidgetHandler viewHandler = new WidgetHandler(context, player);

        if (player.getPlayer() != null &&
            player.getPlayer().getConnection() != null &&
            !player.getPlayer().getConnection().isClosed()) {
            viewHandler.setRunning(true);
        }

        // --- register view handler at player
        player.addHandler(viewHandler);
    }

    public static void updateAllWidgets(Context context, RemoteViews views) {
        final int N = appWidgetIds.length;
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

}
