package sample

import spock.lang.Specification

class SampleTest extends Specification{

    def "Check sample"() {

        expect:
        new Sample().foo() == 'test'
    }
}