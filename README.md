# c.digital

C.Digital is an Android application that is intended as a proof of concept for automatically digitize vote counting result data from photo of vote counting forms. 

## Background

Election in Indonesia is stil very much a manual process. Voting is conducted by marking a paper ballot then place it in a box. At the end of the voting period, each voting station will conduct manual vote counting by examining the ballots one by one and marking the result in an A1-sized form (call C.Hasil form) using tally diagrams. 

In the past, digitization of the voting result from the voting stations takes a lot of effort. Personnels at the voting stations will manually make multiple copies the results in smaller forms with A4 size for distribution. One of the copy is intended for regional Voting Committee (KPU Kabupaten/Kota) where it will be scanned then manually digitized by operators and entered into central database. With hundreds of thousands of voting stations, this takes weeks and thousands of personnels. 

## Solution

In order to speed up the process and also to reduce the effort, it is desired to have the digitization process directly at each voting stations and performed by the local team. An Android mobile app is considered a feasible solution as Android mobile is widely available and nowadays it has sufficient processing power and camera quality. The next goal is to have an OCR capability that will help the voting stations personnel perform digitization with optimal accuracy yet without making their life more difficult. To achieve this, the vote counting forms needs to be redesigned so that it is more OCR-friendly.

This project provides both the form designs and a working implementation of mobile app with OCR capability that is quite robust.

## Prerequisite

C.Digital App requires [OpenCV for Android Library](https://opencv.org/android/). The library needs to be imported into the Android Studio project as a module named "opencv".

