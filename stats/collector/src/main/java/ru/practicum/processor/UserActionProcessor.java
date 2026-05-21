package ru.practicum.processor;

import ru.practicum.ewm.stats.proto.UserActionProto;

public interface UserActionProcessor {
    void process(UserActionProto userActionProto);
}
