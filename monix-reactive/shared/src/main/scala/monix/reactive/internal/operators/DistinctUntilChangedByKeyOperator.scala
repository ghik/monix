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

import cats.Eq
import monix.execution.Ack
import monix.execution.Ack.{ Continue, Stop }
import monix.reactive.Observable.Operator

import scala.util.control.NonFatal
import monix.reactive.observers.Subscriber

import scala.concurrent.Future

private[reactive] final class DistinctUntilChangedByKeyOperator[A, K](key: A => K)(implicit K: Eq[K])
  extends Operator[A, A] {

  def apply(out: Subscriber[A]): Subscriber[A] =
    new Subscriber[A] {
      implicit val scheduler = out.scheduler

      private[this] var isDone = false
      private[this] var isFirst = true
      private[this] var lastKey: K = _

      def onNext(elem: A): Future[Ack] = {
        // Protects calls to user code from within the operator and
        // stream the error downstream if it happens, but if the
        // error happens because of calls to `onNext` or other
        // protocol calls, then the behavior should be undefined.
        var streamErrors = true
        try {
          val k = key(elem)

          if (isFirst) {
            lastKey = k
            isFirst = false
            streamErrors = false
            out.onNext(elem)
          } else if (K.neqv(lastKey, k)) {
            lastKey = k
            streamErrors = false
            out.onNext(elem)
          } else {
            Continue
          }
        } catch {
          case NonFatal(ex) if streamErrors =>
            onError(ex)
            Stop
        }
      }

      def onError(ex: Throwable): Unit =
        if (!isDone) {
          isDone = true
          out.onError(ex)
        }

      def onComplete(): Unit =
        if (!isDone) {
          isDone = true
          out.onComplete()
        }
    }
}
