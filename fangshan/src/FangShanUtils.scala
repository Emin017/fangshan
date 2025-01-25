// SPDX-License-Identifier: MulanPSL-2.0
// SPDX-FileCopyrightText: 2025 Emin <cchuqiming@gmail.com>

package fangshan.utils

import chisel3._

import scala.collection.immutable.SeqMap

object FangShanUtils {

  /** Connect clock and reset to elements */
  def withClockAndReset(element: SeqMap[String, Data], clock: Clock, reset: Reset): Iterable[Unit] = {
    element.map { case (name, element) =>
      name match {
        case "clock" => element.asInstanceOf[Clock] := clock
        case "reset" => element.asInstanceOf[Reset] := reset
        case _       => ()
      }
    }
  }

  /** Set all inputs to DontCare except for ignorePorts */
  def dontCareInputs(element: SeqMap[String, Data], ignorePorts: Seq[String]): Iterable[Unit] = {
    ignorePorts.map { s =>
      element.map { case (name, element) =>
        name match {
          case s => element := DontCare
          case _ => ()
        }
      }
    }
  }
}
