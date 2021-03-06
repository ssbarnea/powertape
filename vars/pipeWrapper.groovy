/*

  pipeWrapper() is a block that can be used to wrap your entire pipelines into
  in order to avoid too many nested block.

  It would help hiding a lot of boilerplate code from your main pipeline source
  by adding:

  - ansiColor wrapper
  - timestapmps wrapper
  - try/catch/final block
  - notification emails

  Parameters (all optional)

  - email: false
  - emailPlain: false

*/

def call(Map args, Closure body) {
  result = {}
  // xterm is needed to be able to configure colors in Jenkins
  ansiColor('xterm') {
    timestamps {

      try {
        body.call()
      }
      catch (ex) {
          log "Exception ${ex.getClass()} received: ${ex}", level: 'FATAL'
          currentBuild.result = 'FAILURE'
      }
      finally {

        msg = getBuildMessage()
        result['buildMessage'] = msg

        // do some report, like sending emails
        if (args.get('email', false)) {
          notifyBuild() // implies format: 'html'
        }
        if (args.get('emailPlain', false)) {
          notifyBuild(format: 'text')
        }
      }

    } // timestamps
  } // ansiColor
  return result
} // pipeWrapper

def call(Closure body) {
  return pipeWrapper([:] ,body)
}
