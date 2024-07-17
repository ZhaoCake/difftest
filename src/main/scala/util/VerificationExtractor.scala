/***************************************************************************************
 * Copyright (c) 2020-2024 Institute of Computing Technology, Chinese Academy of Sciences
 *
 * DiffTest is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 *
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 *
 * See the Mulan PSL v2 for more details.
 ***************************************************************************************/

package difftest.util

import firrtl._
import firrtl.annotations._
import firrtl.ir._
import firrtl.options.Phase
import firrtl.passes.wiring.{SinkAnnotation, SourceAnnotation}
import firrtl.stage.FirrtlCircuitAnnotation

// This is the main user interface for defining the Verification sink.
// index i (i < 0) is connected with assertions.orR; index i (i >= 0) is connected with assertions(i)
// If any sink is defined, the following VerificationExtractor transform will perform the wiring.
// It's worth noting the assertion sink will not hold its value. It will be sampled by the user clock.
object VerificationExtractor {
  def sink(cond: chisel3.Bool, index: Int): Unit = {
    val clock = chisel3.compatibility.currentClock
    val reset = chisel3.compatibility.currentReset
    require(clock.isDefined || index > 0, "Clock must exist for orR sink")
    require(reset.isDefined || index > 0, "Reset must exist for orR sink")
    chisel3.experimental.annotate(new chisel3.experimental.ChiselAnnotation {
      override def toFirrtl: Annotation = VerificationExtractorSink(cond.toTarget, index, clock, reset)
    })
  }

  def sink(cond: chisel3.Bool): Unit = sink(cond, -1)

  def sink(conds: Seq[chisel3.Bool]): Unit = sink(conds, 0)

  def sink(conds: Seq[chisel3.Bool], offset: Int): Unit = conds.zipWithIndex.foreach(x => sink(x._1, offset + x._2))
}

// This transform converts firrtl.ir.Verification to Wiring when sinks are annotated.
class VerificationExtractor extends Phase {
  // Legacy Chisel versions are not supported.
  require(!chisel3.BuildInfo.version.startsWith("3"), "This transform does not support Chisel 3.")

  implicit class AnnotationSeqHelper(annotations: AnnotationSeq) {
    import scala.reflect.ClassTag

    def extract[T <: Annotation: ClassTag](): (Seq[T], Seq[Annotation]) = {
      val (res, others) = annotations.partition(implicitly[ClassTag[T]].runtimeClass.isInstance)
      (res.asInstanceOf[Seq[T]], others)
    }

    def extractCircuit: (Circuit, Seq[Annotation]) = {
      val (circuitAnno, otherAnnos) = annotations.extract[FirrtlCircuitAnnotation]()
      require(circuitAnno.length == 1, "no circuit?")
      (circuitAnno.head.circuit, otherAnnos)
    }
  }

  override def invalidates(a: Phase) = false

  override def transform(annotations: AnnotationSeq): AnnotationSeq = {
    val (c, annos) = annotations.extractCircuit
    val circuitName = CircuitName(c.main)

    // Wiring the sources and perform an orR for it.
    def transformOrRSink(
      modules: Seq[DefModule],
      sinks: Seq[VerificationExtractorSink],
      sources: Seq[SourceAnnotation],
    ): (Seq[DefModule], Seq[Annotation]) = {
      if (sinks.nonEmpty) {
        require(sinks.length == 1, "cannot have more than one Verification sink")
        val sink = sinks.head
        val (sinkModules, otherModules) = modules.partition(_.name == sink.target.module)
        require(sinkModules.length == 1, "cannot have more than one Verification sink Module")
        require(sinkModules.head.isInstanceOf[Module], "Verification sink must be wrapper in some Module")
        val sinkModule = sinkModules.head.asInstanceOf[Module]
        val (newSinkModule, orRSinkAnnos) = onOrRSinkModule(sinkModule, sink, circuitName, sources)
        (otherModules :+ newSinkModule, orRSinkAnnos)
      } else {
        (modules, Seq())
      }
    }

    // The sink has already been annotated. We replace its annotation with SinkAnnotation.
    // Out-of-range sinks are implicitly removed.
    def transformIndexSink(
      sinks: Seq[VerificationExtractorSink],
      sources: Seq[SourceAnnotation],
    ): Seq[SinkAnnotation] = {
      sinks.filter(_.index < sources.length).map(s => SinkAnnotation(s.target, sources(s.index).pin))
    }

    val (sinkAnnos, otherAnnos) = AnnotationSeq(annos).extract[VerificationExtractorSink]()
    // This transform runs only when any sink is defined
    if (sinkAnnos.nonEmpty) {
      // Extract the Verification IRs and convert them into Sources
      val (sourceAnnosSeq, modules) = c.modules.map(m => onSourceModule(m, circuitName)).unzip
      val sourceAnnos = sourceAnnosSeq.flatten
      // Connect the Sources to the Sink modules
      val (orRSinks, indexSinks) = sinkAnnos.partition(_.index < 0)
      val (allModules, orRSinkAnnos) = transformOrRSink(modules, orRSinks, sourceAnnos)
      val indexSinkAnnos = transformIndexSink(indexSinks, sourceAnnos)
      // If there is no orRSink, we need to remove the source annotations never used by any index sink.
      val usedSourceAnnos = sourceAnnos.filter(a => indexSinkAnnos.exists(_.pin == a.pin) || orRSinkAnnos.nonEmpty)
      val allAnnos = otherAnnos ++ usedSourceAnnos ++ orRSinkAnnos ++ indexSinkAnnos
      FirrtlCircuitAnnotation(c.copy(modules = allModules)) +: allAnnos
    } else {
      annotations
    }
  }

  private def onSourceModule(m: DefModule, c: CircuitName): (Seq[SourceAnnotation], DefModule) = {
    m match {
      case Module(info, name, ports, body) =>
        val gen = new AssertionRegGenerator(ModuleName(name, c))
        val (regDefs, newBody) = onStmt(body)(gen)
        val (tailStmt, sourceAnnos) = gen.collect()
        (sourceAnnos, Module(info, name, ports, Block(regDefs :+ newBody :+ tailStmt)))
      case other: DefModule => (Seq(), other)
    }
  }

  private def onStmt(statement: Statement)(implicit regGen: AssertionRegGenerator): (Seq[Statement], Statement) = {
    statement match {
      // TODO: Verification should have an implicit reset such that we can use it as the reset for this Reg.
      case v: Verification if v.op == Formal.Assert =>
        val (reg, regRef) = regGen.next(v.clk)
        val conn0 = Connect(NoInfo, regRef, UIntLiteral(0))
        val conn1 = Connect(NoInfo, regRef, UIntLiteral(1))
        // Original:
        //   assert(v.pred) // note: this is still preserved after the transform
        // Current:
        //   assert_reg := false.B
        //   when (v.pred) { } else { assert_reg := true.B }
        val cond = Conditionally(NoInfo, v.pred, EmptyStmt, conn1)
        (Seq(reg, conn0), Block(v, cond))
      case Conditionally(info, pred, conseq, alt) =>
        val (regs1, s1) = onStmt(conseq)
        val (regs2, s2) = onStmt(alt)
        (regs1 ++ regs2, Conditionally(info, pred, s1, s2))
      case Block(stmts) =>
        val (regs, s) = stmts.map(onStmt).unzip
        (regs.flatten, Block(s.filter(_ != EmptyStmt)))
      case x => (Seq(), x)
    }
  }

  private def onOrRSinkModule(
    m: Module,
    verificationSink: VerificationExtractorSink,
    circuitName: CircuitName,
    sources: Seq[SourceAnnotation],
  ): (DefModule, Seq[SinkAnnotation]) = {
    val target = verificationSink.target.name
    val clock = Reference(verificationSink.clock.get.toTarget.ref, ClockType)
    val reset = Reference(verificationSink.reset.get.toTarget.ref, ResetType)
    val (sinkDefRegs, sinkDefRefs, sinkAnnos) = sources.map { case SourceAnnotation(_, pin) =>
      val (defReg, ref) = DefRegisterWithRef(NoInfo, pin, UIntType(IntWidth(1)), clock, reset, UIntLiteral(0))
      val conn = Connect(NoInfo, ref, UIntLiteral(0))
      val anno = SinkAnnotation(ComponentName(pin, ModuleName(m.name, circuitName)), pin)
      (Block(defReg, conn), ref, anno)
    }.unzip3
    val concat = sinkDefRefs.reduceLeft((result: Expression, sinkRef: Reference) =>
      DoPrim(PrimOps.Cat, Seq(sinkRef, result), Seq(), UIntType(IntWidth(1)))
    )
    val orReduce = DoPrim(PrimOps.Orr, Seq(concat), Seq(), UIntType(IntWidth(1)))
    val conn = Connect(NoInfo, Reference(target, UIntType(IntWidth(1))), orReduce)
    (m.copy(body = Block(m.body +: sinkDefRegs :+ conn)), sinkAnnos)
  }
}

private case class VerificationExtractorSink(
  target: ReferenceTarget,
  index: Int,
  clock: Option[chisel3.Clock],
  reset: Option[chisel3.Reset],
) extends SingleTargetAnnotation[ReferenceTarget] {
  override def duplicate(n: ReferenceTarget): Annotation = this.copy(n)
}

private class AssertionRegGenerator(moduleName: ModuleName) {
  private val clocks = scala.collection.mutable.ListBuffer.empty[Connect]
  private val annos = scala.collection.mutable.ListBuffer.empty[SourceAnnotation]

  def next(clock: Expression): (Statement, Reference) = {
    val name = s"assertion_gen_${annos.length}"
    // We add this clockWire because the clock Expression may not exist at the beginning of the module.
    // Its connection is put at the end of the module after any nodes in the module body.
    // This may not work if the clock is defined within some When scope. We should fix it in the future.
    val clockWire = DefWire(NoInfo, s"${name}_clock", ClockType)
    val clockRef = Reference(clockWire.name, clockWire.tpe)
    clocks.append(Connect(NoInfo, clockRef, clock))
    val (defReg, defRegRef) = DefRegisterWithRef(NoInfo, name, UIntType(IntWidth(1)), clockRef)
    annos.append(SourceAnnotation(ComponentName(defRegRef.name, moduleName), s"${moduleName.name}_$name"))
    (Block(clockWire, defReg), defRegRef)
  }

  def collect(): (Statement, Seq[SourceAnnotation]) = (Block(clocks.toSeq), annos.toSeq)
}

// Return DefRegister as well as the Reference
private object DefRegisterWithRef {
  def apply(
    info: Info,
    name: String,
    tpe: Type,
    clock: Expression,
    reset: Expression,
    init: Expression,
  ): (Statement, Reference) = {
    val defReg = difftest.compatibility.DefRegisterWithReset(info, name, tpe, clock, reset, init)
    (defReg, Reference(name, tpe))
  }

  def apply(
    info: Info,
    name: String,
    tpe: Type,
    clock: Expression,
  ): (Statement, Reference) = {
    val defReg = difftest.compatibility.DefRegister(info, name, tpe, clock)
    (defReg, Reference(name, tpe))
  }
}
