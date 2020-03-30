package ck.benchmarks

import scala.language.higherKinds
import cats.Monad
import cats.data.{ Chain, IndexedReaderWriterStateT }
import cats.effect.SyncIO
import cats.implicits._
import cats.mtl.{ ApplicativeAsk, FunctorTell, MonadState }
import ck.benchmarks.FuncInstances.Func
import ck.benchmarks.ZioInstances.ZIOReaderWriterState

object Test {
  case class Env(config: String)
  case class Event(name: String)
  case class State(value: Int)

  type P[+A] = ZIOReaderWriterState[Env, Chain[Event], State, A]
  type P2[A] = IndexedReaderWriterStateT[SyncIO, Env, Chain[Event], State, State, A]
  type P3[A] = Func[Env, Chain[Event], State, A]

  def testReaderWriterState[F[_]: Monad]: IndexedReaderWriterStateT[F, Env, Chain[Event], State, State, Unit] =
    for {
      conf <- IndexedReaderWriterStateT.ask[F, Env, Chain[Event], State].map(_.config)
      _    <- IndexedReaderWriterStateT.tell[F, Env, Chain[Event], State](Chain(Event(s"Env = $conf")))
      _ <- IndexedReaderWriterStateT.modify[F, Env, Chain[Event], State, State](state =>
            state.copy(value = state.value + 1)
          )
    } yield ()

  def testMTL[F[_]: Monad](
    implicit reader: ApplicativeAsk[F, Env],
    writer: FunctorTell[F, Chain[Event]],
    state: MonadState[F, State]
  ): F[Unit] =
    for {
      conf <- reader.ask.map(_.config)
      _    <- writer.tell(Chain(Event(s"Env = $conf")))
      _    <- state.modify(state => state.copy(value = state.value + 1))
    } yield ()
}