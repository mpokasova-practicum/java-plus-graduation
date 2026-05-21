package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.model.UserAction;
import ru.practicum.repository.UserActionRepository;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionServiceImpl implements UserActionService {
    private final UserActionRepository userActionRepository;

    @Override
    public void save(UserAction userAction) {
        Optional<UserAction> maybeUserAction = userActionRepository.findByUserIdAndEventId(
                userAction.getUserId(), userAction.getEventId());

        if (maybeUserAction.isPresent()) {
            UserAction oldUserAction = maybeUserAction.get();
            if (userAction.getUserScore() > oldUserAction.getUserScore()) {
                oldUserAction.setUserScore(userAction.getUserScore());
                userActionRepository.save(oldUserAction);

                log.debug("Обновлен вес пользователя: userId={}, eventId={}, score={}",
                        oldUserAction.getUserId(), oldUserAction.getEventId(), oldUserAction.getUserScore());
            }
        } else {
            userActionRepository.save(userAction);

            log.debug("Добавлено новое действие: userId={}, eventId={}, score={}",
                    userAction.getUserId(), userAction.getEventId(), userAction.getUserScore());
        }
    }
}
