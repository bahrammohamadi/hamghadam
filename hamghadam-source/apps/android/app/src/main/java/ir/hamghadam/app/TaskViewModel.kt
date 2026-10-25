package ir.hamghadam.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.hamghadam.app.data.AppDatabase
import ir.hamghadam.app.data.TaskEntity
import ir.hamghadam.app.data.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TaskRepository(AppDatabase.get(application).taskDao())
    val tasks: StateFlow<List<TaskEntity>> = repository.observeTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addTask(title: String) = viewModelScope.launch { repository.addTask(title) }
    fun toggle(task: TaskEntity) = viewModelScope.launch { repository.setCompleted(task, !task.isCompleted) }
    fun delete(task: TaskEntity) = viewModelScope.launch { repository.delete(task) }
}
