package org.ei.drishti.service.scheduling;

import org.ei.drishti.contract.Schedule;
import org.ei.drishti.domain.FPProductInformation;
import org.ei.drishti.service.ActionService;
import org.ei.drishti.service.scheduling.fpMethodStrategy.FPMethodStrategyFactory;
import org.motechproject.model.Time;
import org.motechproject.scheduletracking.api.service.EnrollmentRequest;
import org.motechproject.scheduletracking.api.service.ScheduleTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.util.Arrays.asList;
import static org.ei.drishti.common.AllConstants.CommonCommCareFields.BOOLEAN_TRUE_COMMCARE_VALUE;
import static org.ei.drishti.scheduler.DrishtiScheduleConstants.ECSchedulesConstants.*;
import static org.ei.drishti.scheduler.DrishtiScheduleConstants.PREFERED_TIME_FOR_SCHEDULES;
import static org.joda.time.LocalDate.parse;

@Service
public class ECSchedulingService {
    private FPMethodStrategyFactory fpMethodStrategyFactory;
    private final ScheduleTrackingService scheduleTrackingService;
    private final ActionService actionService;
    private final Schedule fpFollowupSchedule = new Schedule(EC_SCHEDULE_FP_FOLLOWUP, asList(EC_SCHEDULE_FP_FOLLOWUP_MILESTONE));
    private final Schedule fpReferralFollowupSchedule = new Schedule(EC_SCHEDULE_FP_REFERRAL_FOLLOWUP, asList(EC_SCHEDULE_FP_REFERRAL_FOLLOWUP_MILESTONE));

    @Autowired
    public ECSchedulingService(FPMethodStrategyFactory fpMethodStrategyFactory, ScheduleTrackingService scheduleTrackingService, ActionService actionService) {
        this.fpMethodStrategyFactory = fpMethodStrategyFactory;
        this.scheduleTrackingService = scheduleTrackingService;
        this.actionService = actionService;
    }

    public void registerEC(FPProductInformation fpInfo) {
        fpMethodStrategyFactory.getStrategyFor(fpInfo.currentFPMethod()).registerEC(fpInfo);
    }

    public void fpChange(FPProductInformation fpInfo) {
        fpMethodStrategyFactory.getStrategyFor(fpInfo.previousFPMethod()).unEnrollFromPreviousScheduleAsFPMethodChanged(fpInfo);

        fpMethodStrategyFactory.getStrategyFor(fpInfo.currentFPMethod()).enrollToNewScheduleForNewFPMethod(fpInfo);
    }

    public void renewFPProduct(FPProductInformation fpInfo) {
        fpMethodStrategyFactory.getStrategyFor(fpInfo.currentFPMethod()).renewFPProduct(fpInfo);
    }

    public void fpFollowup(FPProductInformation fpInfo) {
        fpMethodStrategyFactory.getStrategyFor(fpInfo.currentFPMethod()).fpFollowup(fpInfo);
        updateFPFollowupSchedule(fpInfo);
        updateFPReferralFollowupSchedule(fpInfo);
    }

    public void reportFPComplications(FPProductInformation fpInfo) {
        updateFPFollowupSchedule(fpInfo);
        updateFPReferralFollowupSchedule(fpInfo);
    }

    public void reportReferralFollowup(FPProductInformation fpInfo) {
        updateFPFollowupSchedule(fpInfo);
        updateFPReferralFollowupSchedule(fpInfo);
    }

    private void updateFPFollowupSchedule(FPProductInformation fpInfo) {
        if (BOOLEAN_TRUE_COMMCARE_VALUE.equalsIgnoreCase(fpInfo.needsFollowup())) {
            scheduleTrackingService.enroll(new EnrollmentRequest(fpInfo.entityId(), fpFollowupSchedule.name(), new Time(PREFERED_TIME_FOR_SCHEDULES),
                    parse(fpInfo.fpFollowupDate()), null, null, null, null, null));
        } else {
            scheduleTrackingService.fulfillCurrentMilestone(fpInfo.entityId(), fpFollowupSchedule.name(), parse(fpInfo.submissionDate()));
            actionService.markAlertAsClosed(fpInfo.entityId(), fpInfo.anmId(), fpFollowupSchedule.name(), fpInfo.submissionDate());
        }
    }

    private void updateFPReferralFollowupSchedule(FPProductInformation fpInfo) {
        if (BOOLEAN_TRUE_COMMCARE_VALUE.equalsIgnoreCase(fpInfo.needsReferralFollowup())) {
            scheduleTrackingService.enroll(new EnrollmentRequest(fpInfo.entityId(), fpReferralFollowupSchedule.name(), new Time(PREFERED_TIME_FOR_SCHEDULES),
                    parse(fpInfo.fpFollowupDate()), null, null, null, null, null));
        } else {
            scheduleTrackingService.fulfillCurrentMilestone(fpInfo.entityId(), fpReferralFollowupSchedule.name(), parse(fpInfo.submissionDate()));
            actionService.markAlertAsClosed(fpInfo.entityId(), fpInfo.anmId(), fpReferralFollowupSchedule.name(), fpInfo.submissionDate());
        }
    }
}
