Object subclass: #ClassMethodCompilerTest
       category: 'Some-category-1 Some-category-2'.

!ClassMethodCompilerTest methodsFor: 'test-group'!

answerPlease
    'the answer'!

singleArg: arg
    ^ arg. !
!

!ClassMethodCompilerTest class methodsFor: 'test-group'!

concat: val1 and: val2
    ^ val1 , val2. !
!

ClassMethodCompilerTest
