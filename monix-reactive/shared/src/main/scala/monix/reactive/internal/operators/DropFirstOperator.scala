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

import monix.execution.Ack.Continue
import monix.reactive.Observable.Operator
import monix.reactive.observers.Subscriber

private[reactive] final class DropFirstOperator[A](nr: Long) extends Operator[A, A] {

  def apply(out: Subscriber[A]): Subscriber[A] =
    new Subscriber[A] {
      implicit val scheduler = out.scheduler
      private[this] var count = 0L

      def onNext(elem: A) = {
        if (count < nr) {
          count += 1
          Continue
        } else {
          out.onNext(elem)
        }
      }

      def onComplete(): Unit =
        out.onComplete()
      def onError(ex: Throwable): Unit =
        out.onError(ex)
    }
}
