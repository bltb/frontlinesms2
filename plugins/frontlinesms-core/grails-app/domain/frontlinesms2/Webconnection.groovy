package frontlinesms2

import org.apache.camel.*
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.RouteDefinition
import frontlinesms2.camel.exception.*
import frontlinesms2.api.*

@FrontlineApiAnnotations(apiUrl="webconnection")
abstract class Webconnection extends Activity implements FrontlineApi {
	static final String OWNERDETAIL_SUCCESS = 'success'
	static final String OWNERDETAIL_PENDING = 'pending'
	static final String OWNERDETAIL_FAILED = 'failed'
	
	static subFields = ['message_body' : { msg ->
			def keyword = msg.messageOwner?.keywords?.find{ msg.text.toUpperCase().startsWith(it.value) }?.value
			def text = msg.text
			if (keyword?.size() && text.toUpperCase().startsWith(keyword.toUpperCase())) {
				text = text.substring(keyword.size()).trim()
			}
			text
		},
		'message_body_with_keyword' : { msg -> msg.text },
		'message_src_number' : { msg -> msg.src },
		'message_src_name' : { msg -> Contact.findByMobile(msg.src)?.name ?: msg.src },
		'message_timestamp' : { msg -> msg.dateCreated }]
		
	def camelContext
	def webconnectionService
	def appSettingsService
	def urlHelperService
	def grailsLinkGenerator
	enum HttpMethod { POST, GET }
	static String shortName = 'webconnection'
	static String getType() { '' }
	static def implementations = [UshahidiWebconnection, 
			GenericWebconnection]

	// Camel route redelivery config
	static final def retryAttempts = 3 // how many times to retry before giving up
	static final def initialRetryDelay = 10000 // delay before first retry, in milliseconds
	static final def delayMultiplier = 3 // multiplier applied to delay after each failed attempt
	
	/// Variables
	String url
	HttpMethod httpMethod
	String secret
	boolean apiEnabled = false
	static hasMany = [requestParameters:RequestParameter]
	
	static constraints = {
		name(blank:false, maxSize:255, validator: { val, obj ->
			if(obj?.deleted || obj?.archived) return true
			def identical = Webconnection.findAllByNameIlike(val)
			if(!identical) return true
			if (identical.any { it.id != obj.id && !it.archived && !it.deleted }) return false
			return true
			})
		secret(nullable:true)
		url nullable:false, url:true, validator:{ val, obj ->
			if(!(val ==~ 'http(s?)://.*')) {
				return "webconnection.url.error.url.start.with.http"
			}
			if(val.toLowerCase() ==~ 'http(s?)://localhost(:[0-9]+)?(\\/.*)?') {
				return "webconnection.url.error.locahost.invalid.use.ip"
			}
		}
	}
	static mapping = {
		requestParameters cascade: "all-delete-orphan"
		tablePerHierarchy false
	}

	def processKeyword(TextMessage message, Keyword k) {
		this.addToMessages(message)
		this.save(failOnError:true)
		webconnectionService.doUpload(this, message)
	}

	List<RouteDefinition> getRouteDefinitions() {
		return new RouteBuilder() {
			@Override void configure() {}
			List getRouteDefinitions() {
				return [from("seda:activity-${Webconnection.shortName}-${Webconnection.this.id}")
						.beanRef('webconnectionService', 'preProcess')
						.setHeader(Exchange.HTTP_PATH, simple('${header.url}'))
						.onException(Exception)
									.redeliveryDelay(initialRetryDelay)
									.backOffMultiplier(delayMultiplier)
									.maximumRedeliveries(retryAttempts)
									.retryAttemptedLogLevel(LoggingLevel.WARN)
									.handled(true)
									.beanRef('webconnectionService', 'handleException')
									.end()
						.to(Webconnection.this.url)
						.beanRef('webconnectionService', 'postProcess')
						.routeId("activity-${Webconnection.shortName}-${Webconnection.this.id}")]
			}
		}.routeDefinitions
	}

	def activate() {
		println "*** ACTIVATING ACTIVITY ***"
		webconnectionService.activate(this)
	}

	def deactivate() {
		webconnectionService.deactivate(this)
	}

	abstract def initialize(params)
	abstract def getServiceType()

	def preProcess(Exchange x) {
		println "x: ${x}"
		println "x.in: ${x.in}"
		println "x.in.headers: ${x.in.headers}"
		def textMessage = TextMessage.get(x.in.headers.'fmessage-id')
		def encodedParameters = this.requestParameters.collect {
			urlEncode(it.name) + '=' + urlEncode(webconnectionService.getProcessedValue(it, textMessage))
		}.join('&')
		println "PARAMS:::$encodedParameters"
		x.in.headers[Exchange.HTTP_PATH] = this.url
		x.in.headers[Exchange.HTTP_METHOD] = this.httpMethod
		switch(this.httpMethod) {
			case 'GET':
				x.in.headers[Exchange.HTTP_QUERY] = encodedParameters
				break;
			case 'POST':
				x.in.body = encodedParameters
				x.in.headers[Exchange.CONTENT_TYPE] = 'application/x-www-form-urlencoded'
				break;
		}
		println "# Exchange after adding other headers in the Webconnection.preProcess()"
		println "x.in.headers = $x.in.headers"
		println "x.in.body = $x.in.body"
	}

	void postProcess(Exchange x) {
		println "###### Webconnection.postProcess() with Exchange # ${x}"
		println "Web Connection Response::\n ${x.in.body}"
		log.info "Web Connection Response::\n ${x.in.body}"
	}

	private String urlEncode(String s) throws UnsupportedEncodingException {
		println "PreProcessor.urlEncode : s=$s -> ${URLEncoder.encode(s, "UTF-8")}"
		return URLEncoder.encode(s, "UTF-8");
	}

	//> FrontlineAPI methods
	def apiProcess(controller) {
		//TODO: CORE-1639
		webconnectionService.apiProcess(this, controller)
	}

	def getMoreActions() { ['retryFailed'] }

	String getFullApiUrl(request) {
		def entityClassApiUrl = Webconnection.getAnnotation(FrontlineApiAnnotations.class)?.apiUrl()
 		def path = grailsLinkGenerator.link(controller: 'api', params:[entityClassApiUrl: entityClassApiUrl, entityId: id], absolute: false)
		return apiEnabled? "${urlHelperService.getBaseUrl(request)}$path" : ""
	}
}
	
