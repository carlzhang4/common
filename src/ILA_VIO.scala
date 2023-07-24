package common
import chisel3._
import chisel3.util._
import firrtl.options.CustomFileEmission
import firrtl.annotations.NoTargetAnnotation
import firrtl.annotations.SingleTargetAnnotation
import firrtl.annotations.ReferenceTarget
import firrtl.AnnotationSeq
import firrtl.options.Viewer
import chisel3.stage.ChiselOptions
import chisel3.experimental.{annotate,ChiselAnnotation}
import java.io._

class Pack{
	var targets = Seq[String]()
}
object AnnotationCount{
	var count = 0
}

object DebugFileName{
	var enabled = false
	var str		= ""
}

class BaseILA(seq:Seq[Data])extends BlackBox{
	val t = seq.map(chiselTypeOf(_))
	val io = IO(new Bundle{
		val clk                     = Input(Clock())
		val data					= Input(MixedVec(t))
	})
	def connect(clock:Clock) = {
		assert(name.startsWith("ila"))
		io.clk	:= clock
		io.data.zip(seq).foreach(node => node._1 := node._2)
		val p = new Pack
		val count = AnnotationCount.count
		if (!DebugFileName.enabled) {
			annotate(new ChiselAnnotation {def toFirrtl = DebugFileNameAnno()})
			DebugFileName.enabled = true
		}
		seq.foreach(data => annotate(new ChiselAnnotation {def toFirrtl = MyMetadataAnno(count, name, seq.length, p, data.toTarget)}))
		AnnotationCount.count += 1
	}
}

class BaseVIO(seq:Seq[Data])extends BlackBox{
	val t = seq.map(chiselTypeOf(_))
	val	io = IO(new Bundle{
			val clk                     = Input(Clock())
			val data					= Output(MixedVec(t))
		})
	def connect(clock:Clock) = {
		assert(name.startsWith("vio"))
		io.clk	:= clock
		io.data.zip(seq).foreach(node => node._2 := node._1)
		val p = new Pack
		val count = AnnotationCount.count
		if (!DebugFileName.enabled) {
			annotate(new ChiselAnnotation {def toFirrtl = DebugFileNameAnno()})
			DebugFileName.enabled = true
		}
		seq.foreach(data => annotate(new ChiselAnnotation {def toFirrtl = MyMetadataAnno(count, name, seq.length, p, data.toTarget)}))
		AnnotationCount.count += 1
	}
}

case class MyMetadataAnno(count:Int, name:String, l:Int, p:Pack, target:ReferenceTarget) extends SingleTargetAnnotation[ReferenceTarget] {
	def duplicate(n: ReferenceTarget) = this //this.copy(n)
	p.targets = p.targets :+ target.serialize.split('>')(1).replace('.','_').replace('[','_').replace("]","")
	if(l == p.targets.length){
		p.targets.foreach(str => DebugFileName.str += str+"\n")
		DebugFileName.str += name+":\n"
	}
}

case class DebugFileNameAnno() extends NoTargetAnnotation with CustomFileEmission {

	protected def baseFileName(annotations: AnnotationSeq): String = {
		return Viewer.view[ChiselOptions](annotations).outputFile.get
	}
	protected def suffix: Option[String] = Some(".txt")
	def getBytes: Iterable[Byte] = {
		DebugFileName.enabled = true
		return DebugFileName.str.getBytes()
	}
}