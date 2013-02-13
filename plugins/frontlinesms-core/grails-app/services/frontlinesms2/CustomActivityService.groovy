package frontlinesms2

import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class CustomActivityService {
	/* TODO if you do insist on committing printlns, please make sure they are neat and make sense when
	   reading them on the console.  E.g. include the class and method name at the start, make them precise.
	   Things like the following are not helpful:
			println "<<<>>>"
			println "$it"
	 */
	def saveInstance(customActivity, params) {
		println "customActivity Params ::${params}"
		def steps = new JSONArray(params.jsonToSubmit)
		customActivity.name = params.name
		//TODO DRY the functionality of creating and editing keywords
		customActivity.keywords?.clear()
		println "removing existing steps if any"
		customActivity.steps?.clear()
		
		getSteps(steps).each {
			customActivity.addToSteps(it)
		}

		println "##Just about to save"
		// FIXME why are there multiple saves here?
		customActivity.save(flush:true, failOnError:true)
		println "##Just saved round 1"

		if(params.sorting == 'global'){
			customActivity.addToKeywords(new Keyword(value:''))
		} else if(params.sorting == 'enabled'){
			def keywordRawValues = params.keywords?.toUpperCase().replaceAll(/\s/, "").split(",")
			for(keywordValue in keywordRawValues) {
				def keyword = new Keyword(value: keywordValue.trim().toUpperCase())
				customActivity.addToKeywords(keyword)
			}
		} else {
			println "##### CustomActivityService.saveInstance() # removing keywords"
		}

		println "# 2 ######### Saving Round 2 # $customActivity.errors.allErrors"
		// FIXME why are there multiple saves here?
		customActivity.save(failOnError:true, flush:true)
	}

	private getSteps(steps) {
		def stepInstanceList = []
		println "steps::::: $steps"

		steps.each { step ->
			println "step:::: $step"
			
			def stepInstance = Step.implementations.find {it.shortName == step.stepType}.newInstance(step)
			stepInstance.save()
			step.each { k,v ->
				def stepProp
				if(!(k in ["stepType", "stepId"])) {
					stepProp = new StepProperty(key:k, value:v)
					println "$stepProp::: ${stepProp.key}: ${stepProp.value}"
					stepInstance.addToStepProperties(stepProp)
					println "$stepInstance::: ${stepInstance.stepProperties}"
				}

			}
			stepInstanceList << stepInstance
		}
		stepInstanceList.each {
			println "<<<>>>"
			println "$it"
			println "${it.stepProperties*.key}"
			println "<<<>>>"
		}
		stepInstanceList
	}

	def triggerSteps(c, message) {
		println "c::: ${c}, message::: $message"
		println "c steps::: ${c.steps}"
		c.steps.each {
			println "calling process on $it"
			it.process(message)
		}
	}
}
