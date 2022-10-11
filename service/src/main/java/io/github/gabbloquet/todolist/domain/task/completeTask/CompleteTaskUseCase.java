package io.github.gabbloquet.todolist.domain.task.completeTask;

import io.github.gabbloquet.todolist.annotations.DomainService;
import io.github.gabbloquet.todolist.domain.task.model.TaskEventBus;
import io.github.gabbloquet.todolist.domain.todolist.model.TodolistCommandReceiver;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;

import java.time.Clock;
import java.time.LocalDateTime;

@DomainService
@RequiredArgsConstructor
public class CompleteTaskUseCase implements TodolistCommandReceiver<CompleteTask> {

    @NonNull
    private final TaskEventBus taskEventBus;

    @NonNull
    private final Clock clock;

    @Override
    @EventListener
    public void execute(CompleteTask command) {
        TaskCompleted event = TaskCompleted.builder()
                .taskId(command.taskId)
                .at(LocalDateTime.now(clock))
                .build();

        taskEventBus.publish(event);
    }
}
