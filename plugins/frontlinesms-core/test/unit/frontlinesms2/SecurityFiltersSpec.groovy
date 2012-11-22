package frontlinesms2

import spock.lang.*

import grails.test.mixin.*

@TestFor(SecurityFilters)
@Mock([Group, GroupController, AppSettingsService])
class SecurityFiltersSpec extends Specification {
	def controller
	def appSettingsService

	def setup() {
		appSettingsService = new AppSettingsService()
		appSettingsService.load()
		SecurityFilters.metaClass.appSettingsService = appSettingsService
		appSettingsService.set("auth.basic.enabled", true)
		controller = new GroupController()
	}

	def "should prevents users from accessing the application pages when authentication is enabled"() {
		when:
			withFilters(action: 'list') {
				controller.list()
			}
		then:
			response.status == 401
	}

	def "should enable application access when the right credentials are specified"() {
		setup:
			appSettingsService.set("auth.basic.username", "bla".bytes.encodeBase64().toString())
			appSettingsService.set("auth.basic.password", "pass".bytes.encodeBase64().toString())
		when:
			def password = "bla:pass".bytes.encodeBase64().toString()
			request.addHeader('Authorization', password)
			withFilters(action: 'list') {
				controller.list()
			}
		then:
			response.status == 200
	}

	def "disabling password authentication should enable global application access"() {
		setup:
			appSettingsService.set("auth.basic.enabled", false)
			appSettingsService.set("auth.basic.username", "bla")
			appSettingsService.set("auth.basic.password", "pass".bytes.encodeBase64().toString())
		when:
			withFilters(action: 'list') {
				controller.list()
			}
		then:
			response.status == 200
	}
}
