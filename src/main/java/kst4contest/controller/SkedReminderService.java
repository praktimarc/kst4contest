package kst4contest.controller;

import javafx.application.Platform;
import kst4contest.model.ChatCategory;
import kst4contest.model.ThreadStateMessage;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Schedules PM reminders for a specific sked time.
 *
 * Requirements:
 * - Reminder goes out as PM to the station (via "/cq CALL ...").
 * - Reminders are armed manually from FurtherInfo.
 */
public final class SkedReminderService {

    private final ChatController controller;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("SkedReminderService");
        return t;
    });

    private final ConcurrentHashMap<String, List<ScheduledFuture<?>>> scheduledByCallRaw = new ConcurrentHashMap<>();

    public SkedReminderService(ChatController controller) {
        this.controller = controller;
    }

    /**
     * Arms reminders for one sked. Existing reminders for this call are cancelled.
     *
     * @param callSignRaw target call
     * @param preferredCategory where to send (if null, controller resolves via lastInbound category)
     * @param skedTimeEpochMs sked time
     * @param offsetsMinutes e.g. [5,2,1] => reminders 5,2,1 minutes before
     */
    public void armReminders(String callSignRaw,
                             ChatCategory preferredCategory,
                             long skedTimeEpochMs,
                             List<Integer> offsetsMinutes) {

        String callRaw = normalize(callSignRaw);
        if (callRaw == null || callRaw.isBlank()) return;

        cancelReminders(callRaw);

        long now = System.currentTimeMillis();
        List<Integer> offsets = (offsetsMinutes == null) ? List.of() : offsetsMinutes;

        List<ScheduledFuture<?>> futures = new ArrayList<>();
        for (Integer offMin : offsets) {
            if (offMin == null) continue;

            long fireAt = skedTimeEpochMs - (offMin * 60_000L);
            long delayMs = fireAt - now;
            if (delayMs <= 0) continue;

            ScheduledFuture<?> f = scheduler.schedule(
                    () -> fireReminder(callRaw, preferredCategory, offMin),
                    delayMs,
                    TimeUnit.MILLISECONDS
            );
            futures.add(f);
        }

        scheduledByCallRaw.put(callRaw, futures);

        controller.onThreadStatus("SkedReminderService",
                new ThreadStateMessage("SkedReminder", true,
                        "Armed for " + callRaw + " (" + offsets + " min before)", false));
    }

    public void cancelReminders(String callSignRaw) {
        String callRaw = normalize(callSignRaw);
        if (callRaw == null || callRaw.isBlank()) return;

        List<ScheduledFuture<?>> futures = scheduledByCallRaw.remove(callRaw);
        if (futures != null) {
            for (ScheduledFuture<?> f : futures) {
                if (f != null) f.cancel(false);
            }
        }
    }

    private void fireReminder(String callRaw, ChatCategory preferredCategory, int minutesBefore) {
        try {
            controller.queuePrivateCqMessage(callRaw, preferredCategory, "[KST4C Autoreminder] sked in " + minutesBefore + " min");
            controller.fireUiReminderEvent(callRaw, minutesBefore); //triggers some blingbling in the UI

            ///Local acoustic hint (reuse existing project audio utilities, no AWT, no extra JavaFX modules)

            try {
                if (controller.getChatPreferences().isNotify_playSimpleSounds()) {
                    controller.getPlayAudioUtils().playNoiseLauncher('!'); // choose a suitable char you already use
                }
                // Optional: voice/cw hint (short, not too intrusive)
                // controller.getPlayAudioUtils().playCWLauncher(" SKED " + minutesBefore);
            } catch (Exception ignore) {
                // never block reminder sending because of audio issues
            }

            controller.onThreadStatus("SkedReminderService",
                    new ThreadStateMessage("SkedReminder", true,
                            "PM reminder sent to " + callRaw + " (" + minutesBefore + " min)", false));
        } catch (Exception e) {
            controller.onThreadStatus("SkedReminderService",
                    new ThreadStateMessage("SkedReminder", false,
                            "ERROR sending reminder to " + callRaw + ": " + e.getMessage(), true));
            e.printStackTrace();
        }
    }

    private static String normalize(String s) {
        if (s == null) return null;
        return s.trim().toUpperCase();
    }
}
