Object subclass: #ClassMethodCompilerTest
       category: 'Some-category-1 Some-category-2'.

!ClassMethodCompilerTest methodsFor: 'test-group'!

answerPlease
    'the answer'!
!

!ClassMethodCompilerTest class methodsFor: 'test-group'!

sum: val1 and: val2
    ^ val1 + val2. !
!

ClassMethodCompilerTest
