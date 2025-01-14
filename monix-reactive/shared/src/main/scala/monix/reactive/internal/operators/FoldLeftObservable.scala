/*
 * Copyright (c) 2014-2022 Monix Contributors.
 * See the project homepage at: https://monix.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package monix.reactive.internal.operators

import monix.execution.Ack.{ Continue, Stop }
import scala.util.control.NonFatal
import monix.execution.{ Ack, Cancelable }
import monix.reactive.Observable
import monix.reactive.observers.Subscriber

private[reactive] final class FoldLeftObservable[A, R](source: Observable[A], initial: () => R, f: (R, A) => R)
  extends Observable[R] {

  def unsafeSubscribeFn(out: Subscriber[R]): Cancelable = {
    var streamErrors = true
    try {
      val initialState = initial()
      streamErrors = false

      source.unsafeSubscribeFn(new Subscriber.Sync[A] {
        implicit val scheduler = out.scheduler
        private[this] var isDone = false
        private[this] var state: R = initialState

        def onNext(elem: A): Ack = {
          // Protects calls to user code from within the operator,
          // as a matter of contract.
          try {
            state = f(state, elem)
            Continue
          } catch {
            case ex if NonFatal(ex) =>
              onError(ex)
              Stop
          }
        }

        def onComplete(): Unit =
          if (!isDone) {
            isDone = true
            out.onNext(state)
            out.onComplete()
          }

        def onError(ex: Throwable): Unit =
          if (!isDone) {
            isDone = true
            out.onError(ex)
          }
      })
    } catch {
      case NonFatal(ex) if streamErrors =>
        out.onError(ex)
        Cancelable.empty
    }
  }
}
