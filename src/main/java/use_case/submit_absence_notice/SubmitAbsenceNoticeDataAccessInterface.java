package use_case.submit_absence_notice;

import java.util.List;
import entity.AbsenceNotice;
import entity.Event;
import entity.Team;
import entity.User;

/** Reads the parent link and roster before saving advance absence notice. */
public interface SubmitAbsenceNoticeDataAccessInterface {
    User get(String username);
    Team getTeam(String teamId);
    Event getEvent(String eventId);
    List<String> getLinkedPlayerUsernames(String parentUsername);
    void saveAbsenceNotice(AbsenceNotice notice);
}
