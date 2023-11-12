package kst4contest.test;

import kst4contest.ApplicationConstants;
import kst4contest.controller.DBController;
import kst4contest.controller.Utils4KST;
import kst4contest.model.ChatMember;
import kst4contest.utils.ApplicationFileUtils;

public class TestDBController {

    public static final String DATABASE_FILE = "praktiKST.db";
    private static final String DB_PATH = ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, DATABASE_FILE);


    public static void main(String[] args) {


        DBController dbc = DBController.getInstance();
//        dbc.initDBConnection();

        ChatMember dummy = new ChatMember();
        dummy.setCallSign("DM5M");
        dummy.setQra("jo51ij");
        dummy.setName("Team Test");
        dummy.setLastActivity(new Utils4KST().time_generateActualTimeInDateFormat());
        dummy.setWorked5600(true);
//		dummy.setWorked432(true);

        dbc.fetchChatMemberWkdDataForOnlyOneCallsignFromDB(dummy);


    }
}
