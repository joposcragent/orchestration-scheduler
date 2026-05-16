package ru.sadovskie.leo.app.joposcragent.schedulersvc.service

import org.springframework.stereotype.Service
import ru.sadovskie.leo.app.joposcragent.schedulersvc.cache.SchedulerCache
import ru.sadovskie.leo.app.joposcragent.schedulersvc.domain.BadRequestException
import ru.sadovskie.leo.app.joposcragent.schedulersvc.domain.JobTypeHelper

@Service
class SchedulerExecuteService(
	private val cache: SchedulerCache,
	private val asyncExecutor: SchedulerAsyncExecutor,
) {
	fun execute(jobTypeQuery: String?) {
		val code = JobTypeHelper.resolveJobTypeCode(jobTypeQuery)
		if (cache.get(code) == null) {
			throw BadRequestException(
				"Для указанного jobType нет записи в оперативном кэше планировщика. " +
					"Создайте строку расписания в БД (например через PUT настроек) и убедитесь, что сервис её загрузил.",
			)
		}
		asyncExecutor.executeJob(code)
	}
}
