import io.gatling.recorder.config.RecorderPropertiesBuilder
import io.gatling.recorder.controller.RecorderController

object Recorder extends App {

	val props = new RecorderPropertiesBuilder
	props.simulationOutputFolder(IDEPathHelper.recorderOutputDirectory.toString)
	props.simulationPackage("bd.ciber.gatling")
	props.bodiesFolder(IDEPathHelper.bodiesDirectory.toString)

	RecorderController(props.build, Some(IDEPathHelper.recorderConfigFile))
}