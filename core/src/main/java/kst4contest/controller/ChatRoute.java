package kst4contest.controller;

/** Where an ON4KST chat message belongs, seen from the local station. */
enum ChatRoute {
    /** Receiver field {@code 0}: a message to everyone. */
    PUBLIC,
    /** Addressed to the local login call. */
    TO_ME,
    /** Sent by the local station to someone else. */
    FROM_ME,
    /** Between two other stations. */
    BETWEEN_OTHERS
}
