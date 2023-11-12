package kst4contest.test;

import kst4contest.ApplicationConstants;
import kst4contest.utils.ApplicationFileUtils;

public class TestApplicationFileUtils {
        public static final String DATABASE_FILE = "praktiKST.db";
        private static final String DB_PATH = ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, DATABASE_FILE);

        public static void main(String[] args) {

                System.out.println("DB Path = " + DB_PATH);
    }

}
