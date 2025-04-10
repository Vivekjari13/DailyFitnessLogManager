import kotlinx.coroutines.*
import java.time.LocalDate

suspend fun dailyReminder() {
    try {
        while (true) {
            delay(15000L)
            println("Reminder: Don’t forget to log your activity today!")
        }
    } catch (e: CancellationException) {
        println("Daily reminder stopped.")
    }
}

var fitnessCategories = arrayOf("Cardio", "Strength", "Yoga", "Flexibility", "Running", "Walking")

interface GoalTrackable {
    fun setGoal(minutes: Int)
    fun trackProgress(logs: List<WorkoutLog>)
}

class FitnessLog : GoalTrackable {
    private var logs = mutableListOf<WorkoutLog>()
    private var weeklyGoal = 0

    fun addWorkout(isOutdoor: Boolean) {
        try {
            var id = logs.size + 1
            var name = safeRead("Activity Name: ")
            var category = safeRead("Category (${fitnessCategories.joinToString()}): ")
            var duration = safeRead("Duration (minutes): ").toInt()
            var calories = safeRead("Calories burned: ").toInt()
            var log = if (isOutdoor) {
                var weather = safeRead("Weather Condition: ")
                OutdoorWorkout(id, name, category, duration, calories, LocalDate.now(), weather)
            } else {
                var equipment = safeRead("Gym Equipment Used: ")
                GymWorkout(id, name, category, duration, calories, LocalDate.now(), equipment)
            }
            logs.add(log)
            println("Workout added.")
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }

    fun displayLogs() {
        if (logs.isEmpty()) println("No logs.")
        else logs.forEach { println(it.summary()) }
    }

    fun updateLogByInput(id: Int) {
        var index = logs.indexOfFirst { it.id == id }
        if (index != -1) {
            println("Updating log ID $id")
            try {
                var name = safeRead("New Activity Name: ")
                var category = safeRead("New Category: ")
                var duration = safeRead("New Duration: ").toInt()
                var calories = safeRead("New Calories: ").toInt()
                var date = LocalDate.now()
                var oldLog = logs[index]
                logs[index] = when (oldLog) {
                    is OutdoorWorkout -> {
                        var weather = safeRead("New Weather Condition: ")
                        OutdoorWorkout(id, name, category, duration, calories, date, weather)
                    }
                    is GymWorkout -> {
                        var equipment = safeRead("New Gym Equipment: ")
                        GymWorkout(id, name, category, duration, calories, date, equipment)
                    }
                    else -> WorkoutLog(id, name, category, duration, calories, date)
                }
                println("Updated successfully.")
            } catch (e: Exception) {
                println("Update failed: ${e.message}")
            }
        } else println("Log not found.")
    }

    fun deleteLog(id: Int) {
        var removed = logs.removeIf { it.id == id }
        println(if (removed) "Deleted successfully." else "Log not found.")
    }

    fun showWeeklySummary() {
        var weekLogs = logs.filter { it.date.isAfter(LocalDate.now().minusDays(7)) }
        var total = weekLogs.sumOf { it.duration }
        println("Weekly Summary: ${weekLogs.size} activities, $total minutes logged.")
    }

    fun setAndTrackGoal() {
        if (weeklyGoal == 0) {
            var goal = safeRead("Set your weekly goal (in minutes): ").toIntOrNull() ?: 0
            setGoal(goal)
        }
        trackProgress(logs)
    }

    override fun setGoal(minutes: Int) {
        weeklyGoal = minutes
        println("Goal set to $weeklyGoal minutes.")
    }

    override fun trackProgress(logs: List<WorkoutLog>) {
        var achieved = logs.filter { it.date.isAfter(LocalDate.now().minusDays(7)) }
            .sumOf { it.duration }
        println("You've achieved $achieved/$weeklyGoal minutes this week.")
    }
}

open class WorkoutLog(
    var id: Int,
    private var activityName: String,
    private var category: String,
    var duration: Int,
    private var calories: Int,
    var date: LocalDate
) {
    open fun summary(): String {
        return "$date | [$id] $activityName - $category - $duration minutes - $calories kcal"
    }
}

class OutdoorWorkout(
    id: Int, activityName: String, category: String, duration: Int, calories: Int,
    date: LocalDate, private val weather: String
) : WorkoutLog(id, activityName, category, duration, calories, date) {
    override fun summary(): String {
        return super.summary() + " | Weather: $weather"
    }
}

class GymWorkout(
    id: Int, activityName: String, category: String, duration: Int, calories: Int,
    date: LocalDate, private val equipment: String
) : WorkoutLog(id, activityName, category, duration, calories, date) {
    override fun summary(): String {
        return super.summary() + " | Equipment: $equipment"
    }
}

fun safeRead(prompt: String): String {
    print(prompt)
    return readlnOrNull() ?: throw IllegalArgumentException("Input required")
}

fun main() = runBlocking {
    var fitnessLog = FitnessLog()
    var reminderJob = launch { dailyReminder() }

    var running = true
    while (running) {
        println("\n—— Daily Fitness Log Manager ——")
        println("1. Add Outdoor Workout")
        println("2. Add Gym Workout")
        println("3. View All Logs")
        println("4. Update Log")
        println("5. Delete Log")
        println("6. Show Weekly Summary")
        println("7. Set & Track Fitness Goals")
        println("8. Exit")
        print("Enter your Choice: ")

        when (readln().toInt()) {
            1 -> fitnessLog.addWorkout(isOutdoor = true)
            2 -> fitnessLog.addWorkout(isOutdoor = false)
            3 -> fitnessLog.displayLogs()
            4 -> {
                print("Enter log ID to update: ")
                var id = readln().toInt()
                if (id != null) fitnessLog.updateLogByInput(id)
            }
            5 -> {
                print("Enter log ID to delete: ")
                var id = readln().toInt()
                if (id != null) fitnessLog.deleteLog(id)
            }
            6 -> fitnessLog.showWeeklySummary()
            7 -> fitnessLog.setAndTrackGoal()
            8 -> {
                println("Exit. Stay healthy......")
                reminderJob.cancelAndJoin()
                running = false
            }
            else -> println("Invalid Choice.")
        }
    }
}