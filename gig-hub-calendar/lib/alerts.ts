import { getGigsForAlertCheck, updateGig } from './google-calendar';
import { sendSlackDM } from './slack';

export async function checkAndSendAlerts(): Promise<void> {
  const gigs = await getGigsForAlertCheck();
  const now = new Date();

  for (const gig of gigs) {
    // Ticket sale alert
    if (!gig.ticketSaleAlertSent && gig.ticketSaleDate && gig.ticketSaleDate <= now) {
      try {
        const msg =
          `🎫 Tickets on sale now for *${gig.artist}* at ${gig.location}!` +
          (gig.ticketUrl ? `\n${gig.ticketUrl}` : '');
        await sendSlackDM(msg);
        await updateGig(gig.id, { ticketSaleAlertSent: true });
        console.log(`[alerts] Ticket sale alert sent: ${gig.artist}`);
      } catch (err) {
        console.error(`[alerts] Failed ticket sale alert for ${gig.artist}:`, err);
      }
    }

    // Pre-event reminder
    if (!gig.preEventAlertSent && gig.eventDate > now) {
      const daysUntil = Math.ceil(
        (gig.eventDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24)
      );
      if (daysUntil <= gig.reminderDaysBefore) {
        try {
          const dayWord = daysUntil === 1 ? 'day' : 'days';
          const msg =
            `🎸 Reminder: *${gig.artist}* at ${gig.location} is in ${daysUntil} ${dayWord}!` +
            (gig.ticketUrl ? `\n${gig.ticketUrl}` : '');
          await sendSlackDM(msg);
          await updateGig(gig.id, { preEventAlertSent: true });
          console.log(`[alerts] Pre-event alert sent: ${gig.artist}`);
        } catch (err) {
          console.error(`[alerts] Failed pre-event alert for ${gig.artist}:`, err);
        }
      }
    }
  }
}
