package io.ratpack.operation

import org.apache.commons.lang.exception.ExceptionUtils
import ratpack.exec.Blocking
import ratpack.exec.Operation
import ratpack.exec.Promise
import ratpack.test.exec.ExecHarness
import spock.lang.Specification

class OperationSpec extends Specification {

    /**
     * Uncertain if relevant, but closest test found in RatPack core.
     *
     * Passes?
     * Test is green, but error differs then one thrown.
     * * Expected RuntimeException("Async Error!")
     * * Action "Promise.then() can only be called on a compute thread "
     * Tested in Ratpack Core
     * @see https://github.com/ratpack/ratpack/blob/master/ratpack-core/src/test/groovy/ratpack/exec/PromiseOperationsSpec.groovy#L341
     */
    void 'it should handle an operation failure when using blockingOp'() {
        given:
        Calls calls = new Calls()

        when:
        ExecHarness.runSingle({ e ->
            println "start"
            calls.promiseVoid()
                .blockingOp({
                    calls.asyncIndefinite()
                })
                .result({ r ->
                    if (r.isError()) {
                        println ExceptionUtils.getStackTrace(r.getThrowable())
                        println 'total failure'
                    }
                    println 'end'
                })
        })

        then:
        noExceptionThrown()
    }

    /**
     * Green - But asyncErrorOp never runs
     */
    void 'it should handle an operation failure when using operation'() {
        given:
        Calls calls = new Calls()

        when:
        ExecHarness.runSingle({ e ->
            println "start"
            calls.promiseVoid()
                .flatMap({
                    return calls.asyncErrorOp().promise()
                })
                .result({ r ->
                    if (r.isError()) {
                        println ExceptionUtils.getStackTrace(r.getThrowable())
                        println 'total failure'
                    }
                    println 'end'
                })
        })

        then:
        noExceptionThrown()
    }

    /**
     * Green - But asyncErrorOp never runs
     */
    void 'it should handle an operation failure when using operation 2'() {
        given:
        Calls calls = new Calls()

        when:
        ExecHarness.runSingle({ e ->
            println "start"
            calls.promiseVoid()
                    .operation({
                        //calls.asyncError() // Throws error all the way up
                        //calls.asyncErrorOp() // Never gets invoked
                        calls.error() // Works as expected.
                    })
                    .onError({ t ->
                        println ExceptionUtils.getStackTrace(t)
                        println 'total failure'
                    })
                    .then({
                        println 'done'
                    })
        })

        then:
        noExceptionThrown()
    }

    void 'it should handle an operation failure when using nextOp'() {
        given:
        Calls calls = new Calls()

        when:
        ExecHarness.runSingle({ e ->
            println "start"
            calls.promiseVoid()
                .nextOp({
                    //calls.asyncError() // Throws error all the way up
                    calls.asyncErrorOp() // Never gets invoked
                })
                .result({ r ->
                    if (r.isError()) {
                        println ExceptionUtils.getStackTrace(r.getThrowable())
                        println 'total failure'
                    }
                    println 'end'
                })
        })

        then:
        noExceptionThrown()
    }



    /**
     * Passes - Works as expected.
     * * Exception "RuntimeException("Async Error!")" is caught by .result
     */
    void 'it should handle an operation failure when using Operation.of'() {
        given:
        Calls calls = new Calls()

        when:
        ExecHarness.runSingle({ e ->
            println "start"
            calls.promiseVoid()
                .flatMap({
                    return Operation.of({
                        calls.asyncError()
                    }).promise()
                })
                .result({ r ->
                    if (r.isError()) {
                        println ExceptionUtils.getStackTrace(r.getThrowable())
                        println 'total failure'
                    }
                    println 'end'
                })
        })

        then:
        noExceptionThrown()
    }

    class Calls {

        public Operation asyncErrorOp() {
            return Blocking.get({
                throw new RuntimeException("Async Error!")
            }).operation()
        }

        public void asyncError() {
            Blocking.get({
                throw new RuntimeException("Async Error!")
            }).then({
                println "oops"
            })
        }

        public void error() {
            throw new RuntimeException("Async Error!")
        }

        public void asyncIndefinite() {
            Blocking.get({
                while (true) {}
            }).then({
                println "not so indefinite eh?"
            })
        }

        public Promise<Void> promiseVoid() {
            return Promise.value(null)
        }

        public Promise<String> promiseString() {
            return Promise.value("Hello World!")
        }

    }
}
