package dispatch
package object classic {
  /** Exception listener is called in addition to an HttpExecutor's
   *  regular exception handling (throwing/logging/ignoring it). */
  type ExceptionListener = util.control.Exception.Catcher[Unit]
}
