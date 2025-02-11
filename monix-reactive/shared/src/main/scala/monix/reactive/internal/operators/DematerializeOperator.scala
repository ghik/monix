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

import monix.execution.Ack
import monix.execution.Ack.Stop
import monix.reactive.Notification
import monix.reactive.Notification.{ OnComplete, OnError, OnNext }
import monix.reactive.Observable.Operator
import monix.reactive.observers.Subscriber
import scala.concurrent.Future

private[reactive] final class DematerializeOperator[A] extends Operator[Notification[A], A] {

  def apply(out: Subscriber[A]): Subscriber[Notification[A]] =
    new Subscriber[Notification[A]] {
      implicit val scheduler = out.scheduler
      private[this] var isDone = false

      def onNext(elem: Notification[A]): Future[Ack] = {
        if (isDone) Stop
        else
          elem match {
            case OnNext(e) =>
              out.onNext(e)
            case OnError(ex) =>
              onError(ex)
              Stop
            case OnComplete =>
              onComplete()
              Stop
          }
      }

      def onError(ex: Throwable): Unit = {
        if (!isDone) {
          isDone = true
          out.onError(ex)
        } else {
          scheduler.reportFailure(ex)
        }
      }

      def onComplete(): Unit = {
        if (!isDone) {
          isDone = true
          out.onComplete()
        }
      }
    }
}
