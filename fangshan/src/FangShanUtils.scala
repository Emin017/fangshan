// SPDX-License-Identifier: MulanPSL-2.0
// SPDX-FileCopyrightText: 2025 Emin <cchuqiming@gmail.com>

package fangshan.utils

import chisel3._

import scala.collection.immutable.SeqMap

/** Provide some useful functions, such as:
  *
  * [[withClockAndReset]]: connect clock and reset to elements
  *
  * [[dontCarePorts]]: set all inputs to DontCare except for ignorePorts
  */
object FangShanUtils {

  /** Connect clock and reset to elements
    *
    * @param element
    *   elements to be connected
    * @param clock
    *   clock signal, such as: Clock
    * @param reset
    *   reset signal, such as: Reset
    * @return
    *   Iterable[Unit]
    */
  def withClockAndReset(element: SeqMap[String, Data], clock: Clock, reset: Reset): Iterable[Unit] = {
    element.map { case (name, element) =>
      name match {
        case "clock" => element.asInstanceOf[Clock] := clock
        case "reset" => element.asInstanceOf[Reset] := reset
        case _       => ()
      }
    }
  }

  /** Set all ports to DontCare except for ignorePorts
    *
    * @param element
    *   elements to be set to DontCare
    * @return
    *   Iterable[Unit]
    */
  def dontCarePorts(element: SeqMap[String, Data]): Iterable[Unit] = {
    element.map { case (name, element) =>
      element match {
        case _ => element := DontCare
      }
    }
  }
}
