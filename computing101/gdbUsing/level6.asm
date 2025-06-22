
embryogdb_level6:     file format elf64-x86-64


Disassembly of section .init:

0000000000001000 <_init>:
    1000:	f3 0f 1e fa          	endbr64 
    1004:	48 83 ec 08          	sub    $0x8,%rsp
    1008:	48 8b 05 d9 2f 00 00 	mov    0x2fd9(%rip),%rax        # 3fe8 <__gmon_start__>
    100f:	48 85 c0             	test   %rax,%rax
    1012:	74 02                	je     1016 <_init+0x16>
    1014:	ff d0                	call   *%rax
    1016:	48 83 c4 08          	add    $0x8,%rsp
    101a:	c3                   	ret    

Disassembly of section .plt:

0000000000001020 <.plt>:
    1020:	ff 35 0a 2f 00 00    	push   0x2f0a(%rip)        # 3f30 <_GLOBAL_OFFSET_TABLE_+0x8>
    1026:	f2 ff 25 0b 2f 00 00 	bnd jmp *0x2f0b(%rip)        # 3f38 <_GLOBAL_OFFSET_TABLE_+0x10>
    102d:	0f 1f 00             	nopl   (%rax)
    1030:	f3 0f 1e fa          	endbr64 
    1034:	68 00 00 00 00       	push   $0x0
    1039:	f2 e9 e1 ff ff ff    	bnd jmp 1020 <.plt>
    103f:	90                   	nop
    1040:	f3 0f 1e fa          	endbr64 
    1044:	68 01 00 00 00       	push   $0x1
    1049:	f2 e9 d1 ff ff ff    	bnd jmp 1020 <.plt>
    104f:	90                   	nop
    1050:	f3 0f 1e fa          	endbr64 
    1054:	68 02 00 00 00       	push   $0x2
    1059:	f2 e9 c1 ff ff ff    	bnd jmp 1020 <.plt>
    105f:	90                   	nop
    1060:	f3 0f 1e fa          	endbr64 
    1064:	68 03 00 00 00       	push   $0x3
    1069:	f2 e9 b1 ff ff ff    	bnd jmp 1020 <.plt>
    106f:	90                   	nop
    1070:	f3 0f 1e fa          	endbr64 
    1074:	68 04 00 00 00       	push   $0x4
    1079:	f2 e9 a1 ff ff ff    	bnd jmp 1020 <.plt>
    107f:	90                   	nop
    1080:	f3 0f 1e fa          	endbr64 
    1084:	68 05 00 00 00       	push   $0x5
    1089:	f2 e9 91 ff ff ff    	bnd jmp 1020 <.plt>
    108f:	90                   	nop
    1090:	f3 0f 1e fa          	endbr64 
    1094:	68 06 00 00 00       	push   $0x6
    1099:	f2 e9 81 ff ff ff    	bnd jmp 1020 <.plt>
    109f:	90                   	nop
    10a0:	f3 0f 1e fa          	endbr64 
    10a4:	68 07 00 00 00       	push   $0x7
    10a9:	f2 e9 71 ff ff ff    	bnd jmp 1020 <.plt>
    10af:	90                   	nop
    10b0:	f3 0f 1e fa          	endbr64 
    10b4:	68 08 00 00 00       	push   $0x8
    10b9:	f2 e9 61 ff ff ff    	bnd jmp 1020 <.plt>
    10bf:	90                   	nop
    10c0:	f3 0f 1e fa          	endbr64 
    10c4:	68 09 00 00 00       	push   $0x9
    10c9:	f2 e9 51 ff ff ff    	bnd jmp 1020 <.plt>
    10cf:	90                   	nop
    10d0:	f3 0f 1e fa          	endbr64 
    10d4:	68 0a 00 00 00       	push   $0xa
    10d9:	f2 e9 41 ff ff ff    	bnd jmp 1020 <.plt>
    10df:	90                   	nop
    10e0:	f3 0f 1e fa          	endbr64 
    10e4:	68 0b 00 00 00       	push   $0xb
    10e9:	f2 e9 31 ff ff ff    	bnd jmp 1020 <.plt>
    10ef:	90                   	nop
    10f0:	f3 0f 1e fa          	endbr64 
    10f4:	68 0c 00 00 00       	push   $0xc
    10f9:	f2 e9 21 ff ff ff    	bnd jmp 1020 <.plt>
    10ff:	90                   	nop
    1100:	f3 0f 1e fa          	endbr64 
    1104:	68 0d 00 00 00       	push   $0xd
    1109:	f2 e9 11 ff ff ff    	bnd jmp 1020 <.plt>
    110f:	90                   	nop
    1110:	f3 0f 1e fa          	endbr64 
    1114:	68 0e 00 00 00       	push   $0xe
    1119:	f2 e9 01 ff ff ff    	bnd jmp 1020 <.plt>
    111f:	90                   	nop
    1120:	f3 0f 1e fa          	endbr64 
    1124:	68 0f 00 00 00       	push   $0xf
    1129:	f2 e9 f1 fe ff ff    	bnd jmp 1020 <.plt>
    112f:	90                   	nop
    1130:	f3 0f 1e fa          	endbr64 
    1134:	68 10 00 00 00       	push   $0x10
    1139:	f2 e9 e1 fe ff ff    	bnd jmp 1020 <.plt>
    113f:	90                   	nop
    1140:	f3 0f 1e fa          	endbr64 
    1144:	68 11 00 00 00       	push   $0x11
    1149:	f2 e9 d1 fe ff ff    	bnd jmp 1020 <.plt>
    114f:	90                   	nop
    1150:	f3 0f 1e fa          	endbr64 
    1154:	68 12 00 00 00       	push   $0x12
    1159:	f2 e9 c1 fe ff ff    	bnd jmp 1020 <.plt>
    115f:	90                   	nop

Disassembly of section .plt.got:

0000000000001160 <__cxa_finalize@plt>:
    1160:	f3 0f 1e fa          	endbr64 
    1164:	f2 ff 25 8d 2e 00 00 	bnd jmp *0x2e8d(%rip)        # 3ff8 <__cxa_finalize@GLIBC_2.2.5>
    116b:	0f 1f 44 00 00       	nopl   0x0(%rax,%rax,1)

Disassembly of section .plt.sec:

0000000000001170 <putchar@plt>:
    1170:	f3 0f 1e fa          	endbr64 
    1174:	f2 ff 25 c5 2d 00 00 	bnd jmp *0x2dc5(%rip)        # 3f40 <putchar@GLIBC_2.2.5>
    117b:	0f 1f 44 00 00       	nopl   0x0(%rax,%rax,1)

0000000000001180 <__errno_location@plt>:
    1180:	f3 0f 1e fa          	endbr64 
    1184:	f2 ff 25 bd 2d 00 00 	bnd jmp *0x2dbd(%rip)        # 3f48 <__errno_location@GLIBC_2.2.5>
    118b:	0f 1f 44 00 00       	nopl   0x0(%rax,%rax,1)

0000000000001190 <puts@plt>:
    1190:	f3 0f 1e fa          	endbr64 
    1194:	f2 ff 25 b5 2d 00 00 	bnd jmp *0x2db5(%rip)        # 3f50 <puts@GLIBC_2.2.5>
    119b:	0f 1f 44 00 00       	nopl   0x0(%rax,%rax,1)

00000000000011a0 <readlink@plt>:
    11a0:	f3 0f 1e fa          	endbr64 
    11a4:	f2 ff 25 ad 2d 00 00 	bnd jmp *0x2dad(%rip)        # 3f58 <readlink@GLIBC_2.2.5>
    11ab:	0f 1f 44 00 00       	nopl   0x0(%rax,%rax,1)

00000000000011b0 <write@plt>:
    11b0:	f3 0f 1e fa          	endbr64 
    11b4:	f2 ff 25 a5 2d 00 00 	bnd jmp *0x2da5(%rip)        # 3f60 <write@GLIBC_2.2.5>
    11bb:	0f 1f 44 00 00       	nopl   0x0(%rax,%rax,1)

00000000000011c0 <__stack_chk_fail@plt>:
    11c0:	f3 0f 1e fa          	endbr64 
    11c4:	f2 ff 25 9d 2d 00 00 	bnd jmp *0x2d9d(%rip)        # 3f68 <__stack_chk_fail@GLIBC_2.4>
    11cb:	0f 1f 44 00 00       	nopl   0x0(%rax,%rax,1)

00000000000011d0 <printf@plt>:
    11d0:	f3 0f 1e fa          	endbr64 
    11d4:	f2 ff 25 95 2d 00 00 	bnd jmp *0x2d95(%rip)        # 3f70 <printf@GLIBC_2.2.5>
    11db:	0f 1f 44 00 00       	nopl   0x0(%rax,%rax,1)

00000000000011e0 <snprintf@plt>:
    11e0:	f3 0f 1e fa          	endbr64 
    11e4:	f2 ff 25 8d 2d 00 00 	bnd jmp *0x2d8d(%rip)        # 3f78 <snprintf@GLIBC_2.2.5>
    11eb:	0f 1f 44 00 00       	nopl   0x0(%rax,%rax,1)

00000000000011f0 <__assert_fail@plt>:
    11f0:	f3 0f 1e fa          	endbr64 
    11f4:	f2 ff 25 85 2d 00 00 	bnd jmp *0x2d85(%rip)        # 3f80 <__assert_fail@GLIBC_2.2.5>
    11fb:	0f 1f 44 00 00       	nopl   0x0(%rax,%rax,1)

0000000000001200 <geteuid@plt>:
    1200:	f3 0f 1e fa          	endbr64 
    1204:	f2 ff 25 7d 2d 00 00 	bnd jmp *0x2d7d(%rip)        # 3f88 <geteuid@GLIBC_2.2.5>
    120b:	0f 1f 44 00 00       	nopl   0x0(%rax,%rax,1)

0000000000001210 <read@plt>:
    1210:	f3 0f 1e fa          	endbr64 
    1214:	f2 ff 25 75 2d 00 00 	bnd jmp *0x2d75(%rip)        # 3f90 <read@GLIBC_2.2.5>
    121b:	0f 1f 44 00 00       	nopl   0x0(%rax,%rax,1)

0000000000001220 <execve@plt>:
    1220:	f3 0f 1e fa          	endbr64 
    1224:	f2 ff 25 6d 2d 00 00 	bnd jmp *0x2d6d(%rip)        # 3f98 <execve@GLIBC_2.2.5>
    122b:	0f 1f 44 00 00       	nopl   0x0(%rax,%rax,1)

0000000000001230 <strcmp@plt>:
    1230:	f3 0f 1e fa          	endbr64 
    1234:	f2 ff 25 65 2d 00 00 	bnd jmp *0x2d65(%rip)        # 3fa0 <strcmp@GLIBC_2.2.5>
    123b:	0f 1f 44 00 00       	nopl   0x0(%rax,%rax,1)

0000000000001240 <setvbuf@plt>:
    1240:	f3 0f 1e fa          	endbr64 
    1244:	f2 ff 25 5d 2d 00 00 	bnd jmp *0x2d5d(%rip)        # 3fa8 <setvbuf@GLIBC_2.2.5>
    124b:	0f 1f 44 00 00       	nopl   0x0(%rax,%rax,1)

0000000000001250 <open@plt>:
    1250:	f3 0f 1e fa          	endbr64 
    1254:	f2 ff 25 55 2d 00 00 	bnd jmp *0x2d55(%rip)        # 3fb0 <open@GLIBC_2.2.5>
    125b:	0f 1f 44 00 00       	nopl   0x0(%rax,%rax,1)

0000000000001260 <__isoc99_scanf@plt>:
    1260:	f3 0f 1e fa          	endbr64 
    1264:	f2 ff 25 4d 2d 00 00 	bnd jmp *0x2d4d(%rip)        # 3fb8 <__isoc99_scanf@GLIBC_2.7>
    126b:	0f 1f 44 00 00       	nopl   0x0(%rax,%rax,1)

0000000000001270 <getppid@plt>:
    1270:	f3 0f 1e fa          	endbr64 
    1274:	f2 ff 25 45 2d 00 00 	bnd jmp *0x2d45(%rip)        # 3fc0 <getppid@GLIBC_2.2.5>
    127b:	0f 1f 44 00 00       	nopl   0x0(%rax,%rax,1)

0000000000001280 <exit@plt>:
    1280:	f3 0f 1e fa          	endbr64 
    1284:	f2 ff 25 3d 2d 00 00 	bnd jmp *0x2d3d(%rip)        # 3fc8 <exit@GLIBC_2.2.5>
    128b:	0f 1f 44 00 00       	nopl   0x0(%rax,%rax,1)

0000000000001290 <strerror@plt>:
    1290:	f3 0f 1e fa          	endbr64 
    1294:	f2 ff 25 35 2d 00 00 	bnd jmp *0x2d35(%rip)        # 3fd0 <strerror@GLIBC_2.2.5>
    129b:	0f 1f 44 00 00       	nopl   0x0(%rax,%rax,1)

Disassembly of section .text:

00000000000012a0 <_start>:
    12a0:	f3 0f 1e fa          	endbr64 
    12a4:	31 ed                	xor    %ebp,%ebp
    12a6:	49 89 d1             	mov    %rdx,%r9
    12a9:	5e                   	pop    %rsi
    12aa:	48 89 e2             	mov    %rsp,%rdx
    12ad:	48 83 e4 f0          	and    $0xfffffffffffffff0,%rsp
    12b1:	50                   	push   %rax
    12b2:	54                   	push   %rsp
    12b3:	4c 8d 05 56 0b 00 00 	lea    0xb56(%rip),%r8        # 1e10 <__libc_csu_fini>
    12ba:	48 8d 0d df 0a 00 00 	lea    0xadf(%rip),%rcx        # 1da0 <__libc_csu_init>
    12c1:	48 8d 3d de 07 00 00 	lea    0x7de(%rip),%rdi        # 1aa6 <main>
    12c8:	ff 15 12 2d 00 00    	call   *0x2d12(%rip)        # 3fe0 <__libc_start_main@GLIBC_2.2.5>
    12ce:	f4                   	hlt    
    12cf:	90                   	nop

00000000000012d0 <deregister_tm_clones>:
    12d0:	48 8d 3d 39 2d 00 00 	lea    0x2d39(%rip),%rdi        # 4010 <stdout@GLIBC_2.2.5>
    12d7:	48 8d 05 32 2d 00 00 	lea    0x2d32(%rip),%rax        # 4010 <stdout@GLIBC_2.2.5>
    12de:	48 39 f8             	cmp    %rdi,%rax
    12e1:	74 15                	je     12f8 <deregister_tm_clones+0x28>
    12e3:	48 8b 05 ee 2c 00 00 	mov    0x2cee(%rip),%rax        # 3fd8 <_ITM_deregisterTMCloneTable>
    12ea:	48 85 c0             	test   %rax,%rax
    12ed:	74 09                	je     12f8 <deregister_tm_clones+0x28>
    12ef:	ff e0                	jmp    *%rax
    12f1:	0f 1f 80 00 00 00 00 	nopl   0x0(%rax)
    12f8:	c3                   	ret    
    12f9:	0f 1f 80 00 00 00 00 	nopl   0x0(%rax)

0000000000001300 <register_tm_clones>:
    1300:	48 8d 3d 09 2d 00 00 	lea    0x2d09(%rip),%rdi        # 4010 <stdout@GLIBC_2.2.5>
    1307:	48 8d 35 02 2d 00 00 	lea    0x2d02(%rip),%rsi        # 4010 <stdout@GLIBC_2.2.5>
    130e:	48 29 fe             	sub    %rdi,%rsi
    1311:	48 89 f0             	mov    %rsi,%rax
    1314:	48 c1 ee 3f          	shr    $0x3f,%rsi
    1318:	48 c1 f8 03          	sar    $0x3,%rax
    131c:	48 01 c6             	add    %rax,%rsi
    131f:	48 d1 fe             	sar    %rsi
    1322:	74 14                	je     1338 <register_tm_clones+0x38>
    1324:	48 8b 05 c5 2c 00 00 	mov    0x2cc5(%rip),%rax        # 3ff0 <_ITM_registerTMCloneTable>
    132b:	48 85 c0             	test   %rax,%rax
    132e:	74 08                	je     1338 <register_tm_clones+0x38>
    1330:	ff e0                	jmp    *%rax
    1332:	66 0f 1f 44 00 00    	nopw   0x0(%rax,%rax,1)
    1338:	c3                   	ret    
    1339:	0f 1f 80 00 00 00 00 	nopl   0x0(%rax)

0000000000001340 <__do_global_dtors_aux>:
    1340:	f3 0f 1e fa          	endbr64 
    1344:	80 3d dd 2c 00 00 00 	cmpb   $0x0,0x2cdd(%rip)        # 4028 <completed.8060>
    134b:	75 2b                	jne    1378 <__do_global_dtors_aux+0x38>
    134d:	55                   	push   %rbp
    134e:	48 83 3d a2 2c 00 00 	cmpq   $0x0,0x2ca2(%rip)        # 3ff8 <__cxa_finalize@GLIBC_2.2.5>
    1355:	00 
    1356:	48 89 e5             	mov    %rsp,%rbp
    1359:	74 0c                	je     1367 <__do_global_dtors_aux+0x27>
    135b:	48 8b 3d a6 2c 00 00 	mov    0x2ca6(%rip),%rdi        # 4008 <__dso_handle>
    1362:	e8 f9 fd ff ff       	call   1160 <__cxa_finalize@plt>
    1367:	e8 64 ff ff ff       	call   12d0 <deregister_tm_clones>
    136c:	c6 05 b5 2c 00 00 01 	movb   $0x1,0x2cb5(%rip)        # 4028 <completed.8060>
    1373:	5d                   	pop    %rbp
    1374:	c3                   	ret    
    1375:	0f 1f 00             	nopl   (%rax)
    1378:	c3                   	ret    
    1379:	0f 1f 80 00 00 00 00 	nopl   0x0(%rax)

0000000000001380 <frame_dummy>:
    1380:	f3 0f 1e fa          	endbr64 
    1384:	e9 77 ff ff ff       	jmp    1300 <register_tm_clones>

0000000000001389 <auto_gdb>:
    1389:	f3 0f 1e fa          	endbr64 
    138d:	55                   	push   %rbp
    138e:	48 89 e5             	mov    %rsp,%rbp
    1391:	48 81 ec 40 0b 00 00 	sub    $0xb40,%rsp
    1398:	89 bd dc f4 ff ff    	mov    %edi,-0xb24(%rbp)
    139e:	48 89 b5 d0 f4 ff ff 	mov    %rsi,-0xb30(%rbp)
    13a5:	48 89 95 c8 f4 ff ff 	mov    %rdx,-0xb38(%rbp)
    13ac:	64 48 8b 04 25 28 00 	mov    %fs:0x28,%rax
    13b3:	00 00 
    13b5:	48 89 45 f8          	mov    %rax,-0x8(%rbp)
    13b9:	31 c0                	xor    %eax,%eax
    13bb:	48 c7 85 f0 fc ff ff 	movq   $0x0,-0x310(%rbp)
    13c2:	00 00 00 00 
    13c6:	48 c7 85 f8 fc ff ff 	movq   $0x0,-0x308(%rbp)
    13cd:	00 00 00 00 
    13d1:	48 c7 85 00 fd ff ff 	movq   $0x0,-0x300(%rbp)
    13d8:	00 00 00 00 
    13dc:	48 c7 85 08 fd ff ff 	movq   $0x0,-0x2f8(%rbp)
    13e3:	00 00 00 00 
    13e7:	48 c7 85 10 fd ff ff 	movq   $0x0,-0x2f0(%rbp)
    13ee:	00 00 00 00 
    13f2:	48 c7 85 18 fd ff ff 	movq   $0x0,-0x2e8(%rbp)
    13f9:	00 00 00 00 
    13fd:	48 c7 85 20 fd ff ff 	movq   $0x0,-0x2e0(%rbp)
    1404:	00 00 00 00 
    1408:	48 c7 85 28 fd ff ff 	movq   $0x0,-0x2d8(%rbp)
    140f:	00 00 00 00 
    1413:	48 c7 85 30 fd ff ff 	movq   $0x0,-0x2d0(%rbp)
    141a:	00 00 00 00 
    141e:	48 c7 85 38 fd ff ff 	movq   $0x0,-0x2c8(%rbp)
    1425:	00 00 00 00 
    1429:	48 c7 85 40 fd ff ff 	movq   $0x0,-0x2c0(%rbp)
    1430:	00 00 00 00 
    1434:	48 c7 85 48 fd ff ff 	movq   $0x0,-0x2b8(%rbp)
    143b:	00 00 00 00 
    143f:	48 c7 85 50 fd ff ff 	movq   $0x0,-0x2b0(%rbp)
    1446:	00 00 00 00 
    144a:	48 c7 85 58 fd ff ff 	movq   $0x0,-0x2a8(%rbp)
    1451:	00 00 00 00 
    1455:	48 c7 85 60 fd ff ff 	movq   $0x0,-0x2a0(%rbp)
    145c:	00 00 00 00 
    1460:	48 c7 85 68 fd ff ff 	movq   $0x0,-0x298(%rbp)
    1467:	00 00 00 00 
    146b:	48 c7 85 70 fd ff ff 	movq   $0x0,-0x290(%rbp)
    1472:	00 00 00 00 
    1476:	48 c7 85 78 fd ff ff 	movq   $0x0,-0x288(%rbp)
    147d:	00 00 00 00 
    1481:	48 c7 85 80 fd ff ff 	movq   $0x0,-0x280(%rbp)
    1488:	00 00 00 00 
    148c:	48 c7 85 88 fd ff ff 	movq   $0x0,-0x278(%rbp)
    1493:	00 00 00 00 
    1497:	48 c7 85 90 fd ff ff 	movq   $0x0,-0x270(%rbp)
    149e:	00 00 00 00 
    14a2:	48 c7 85 98 fd ff ff 	movq   $0x0,-0x268(%rbp)
    14a9:	00 00 00 00 
    14ad:	48 c7 85 a0 fd ff ff 	movq   $0x0,-0x260(%rbp)
    14b4:	00 00 00 00 
    14b8:	48 c7 85 a8 fd ff ff 	movq   $0x0,-0x258(%rbp)
    14bf:	00 00 00 00 
    14c3:	48 c7 85 b0 fd ff ff 	movq   $0x0,-0x250(%rbp)
    14ca:	00 00 00 00 
    14ce:	48 c7 85 b8 fd ff ff 	movq   $0x0,-0x248(%rbp)
    14d5:	00 00 00 00 
    14d9:	48 c7 85 c0 fd ff ff 	movq   $0x0,-0x240(%rbp)
    14e0:	00 00 00 00 
    14e4:	48 c7 85 c8 fd ff ff 	movq   $0x0,-0x238(%rbp)
    14eb:	00 00 00 00 
    14ef:	48 c7 85 d0 fd ff ff 	movq   $0x0,-0x230(%rbp)
    14f6:	00 00 00 00 
    14fa:	48 c7 85 d8 fd ff ff 	movq   $0x0,-0x228(%rbp)
    1501:	00 00 00 00 
    1505:	48 c7 85 e0 fd ff ff 	movq   $0x0,-0x220(%rbp)
    150c:	00 00 00 00 
    1510:	48 c7 85 e8 fd ff ff 	movq   $0x0,-0x218(%rbp)
    1517:	00 00 00 00 
    151b:	48 c7 85 f0 fd ff ff 	movq   $0x0,-0x210(%rbp)
    1522:	00 00 00 00 
    1526:	48 c7 85 f8 fd ff ff 	movq   $0x0,-0x208(%rbp)
    152d:	00 00 00 00 
    1531:	48 c7 85 00 fe ff ff 	movq   $0x0,-0x200(%rbp)
    1538:	00 00 00 00 
    153c:	48 c7 85 08 fe ff ff 	movq   $0x0,-0x1f8(%rbp)
    1543:	00 00 00 00 
    1547:	48 c7 85 10 fe ff ff 	movq   $0x0,-0x1f0(%rbp)
    154e:	00 00 00 00 
    1552:	48 c7 85 18 fe ff ff 	movq   $0x0,-0x1e8(%rbp)
    1559:	00 00 00 00 
    155d:	48 c7 85 20 fe ff ff 	movq   $0x0,-0x1e0(%rbp)
    1564:	00 00 00 00 
    1568:	48 c7 85 28 fe ff ff 	movq   $0x0,-0x1d8(%rbp)
    156f:	00 00 00 00 
    1573:	48 c7 85 30 fe ff ff 	movq   $0x0,-0x1d0(%rbp)
    157a:	00 00 00 00 
    157e:	48 c7 85 38 fe ff ff 	movq   $0x0,-0x1c8(%rbp)
    1585:	00 00 00 00 
    1589:	48 c7 85 40 fe ff ff 	movq   $0x0,-0x1c0(%rbp)
    1590:	00 00 00 00 
    1594:	48 c7 85 48 fe ff ff 	movq   $0x0,-0x1b8(%rbp)
    159b:	00 00 00 00 
    159f:	48 c7 85 50 fe ff ff 	movq   $0x0,-0x1b0(%rbp)
    15a6:	00 00 00 00 
    15aa:	48 c7 85 58 fe ff ff 	movq   $0x0,-0x1a8(%rbp)
    15b1:	00 00 00 00 
    15b5:	48 c7 85 60 fe ff ff 	movq   $0x0,-0x1a0(%rbp)
    15bc:	00 00 00 00 
    15c0:	48 c7 85 68 fe ff ff 	movq   $0x0,-0x198(%rbp)
    15c7:	00 00 00 00 
    15cb:	48 c7 85 70 fe ff ff 	movq   $0x0,-0x190(%rbp)
    15d2:	00 00 00 00 
    15d6:	48 c7 85 78 fe ff ff 	movq   $0x0,-0x188(%rbp)
    15dd:	00 00 00 00 
    15e1:	48 c7 85 80 fe ff ff 	movq   $0x0,-0x180(%rbp)
    15e8:	00 00 00 00 
    15ec:	48 c7 85 88 fe ff ff 	movq   $0x0,-0x178(%rbp)
    15f3:	00 00 00 00 
    15f7:	48 c7 85 90 fe ff ff 	movq   $0x0,-0x170(%rbp)
    15fe:	00 00 00 00 
    1602:	48 c7 85 98 fe ff ff 	movq   $0x0,-0x168(%rbp)
    1609:	00 00 00 00 
    160d:	48 c7 85 a0 fe ff ff 	movq   $0x0,-0x160(%rbp)
    1614:	00 00 00 00 
    1618:	48 c7 85 a8 fe ff ff 	movq   $0x0,-0x158(%rbp)
    161f:	00 00 00 00 
    1623:	48 c7 85 b0 fe ff ff 	movq   $0x0,-0x150(%rbp)
    162a:	00 00 00 00 
    162e:	48 c7 85 b8 fe ff ff 	movq   $0x0,-0x148(%rbp)
    1635:	00 00 00 00 
    1639:	48 c7 85 c0 fe ff ff 	movq   $0x0,-0x140(%rbp)
    1640:	00 00 00 00 
    1644:	48 c7 85 c8 fe ff ff 	movq   $0x0,-0x138(%rbp)
    164b:	00 00 00 00 
    164f:	48 c7 85 d0 fe ff ff 	movq   $0x0,-0x130(%rbp)
    1656:	00 00 00 00 
    165a:	48 c7 85 d8 fe ff ff 	movq   $0x0,-0x128(%rbp)
    1661:	00 00 00 00 
    1665:	48 c7 85 e0 fe ff ff 	movq   $0x0,-0x120(%rbp)
    166c:	00 00 00 00 
    1670:	48 c7 85 e8 fe ff ff 	movq   $0x0,-0x118(%rbp)
    1677:	00 00 00 00 
    167b:	48 c7 85 f0 fe ff ff 	movq   $0x0,-0x110(%rbp)
    1682:	00 00 00 00 
    1686:	48 c7 85 f8 fe ff ff 	movq   $0x0,-0x108(%rbp)
    168d:	00 00 00 00 
    1691:	48 c7 85 00 ff ff ff 	movq   $0x0,-0x100(%rbp)
    1698:	00 00 00 00 
    169c:	48 c7 85 08 ff ff ff 	movq   $0x0,-0xf8(%rbp)
    16a3:	00 00 00 00 
    16a7:	48 c7 85 10 ff ff ff 	movq   $0x0,-0xf0(%rbp)
    16ae:	00 00 00 00 
    16b2:	48 c7 85 18 ff ff ff 	movq   $0x0,-0xe8(%rbp)
    16b9:	00 00 00 00 
    16bd:	48 c7 85 20 ff ff ff 	movq   $0x0,-0xe0(%rbp)
    16c4:	00 00 00 00 
    16c8:	48 c7 85 28 ff ff ff 	movq   $0x0,-0xd8(%rbp)
    16cf:	00 00 00 00 
    16d3:	48 c7 85 30 ff ff ff 	movq   $0x0,-0xd0(%rbp)
    16da:	00 00 00 00 
    16de:	48 c7 85 38 ff ff ff 	movq   $0x0,-0xc8(%rbp)
    16e5:	00 00 00 00 
    16e9:	48 c7 85 40 ff ff ff 	movq   $0x0,-0xc0(%rbp)
    16f0:	00 00 00 00 
    16f4:	48 c7 85 48 ff ff ff 	movq   $0x0,-0xb8(%rbp)
    16fb:	00 00 00 00 
    16ff:	48 c7 85 50 ff ff ff 	movq   $0x0,-0xb0(%rbp)
    1706:	00 00 00 00 
    170a:	48 c7 85 58 ff ff ff 	movq   $0x0,-0xa8(%rbp)
    1711:	00 00 00 00 
    1715:	48 c7 85 60 ff ff ff 	movq   $0x0,-0xa0(%rbp)
    171c:	00 00 00 00 
    1720:	48 c7 85 68 ff ff ff 	movq   $0x0,-0x98(%rbp)
    1727:	00 00 00 00 
    172b:	48 c7 85 70 ff ff ff 	movq   $0x0,-0x90(%rbp)
    1732:	00 00 00 00 
    1736:	48 c7 85 78 ff ff ff 	movq   $0x0,-0x88(%rbp)
    173d:	00 00 00 00 
    1741:	48 c7 45 80 00 00 00 	movq   $0x0,-0x80(%rbp)
    1748:	00 
    1749:	48 c7 45 88 00 00 00 	movq   $0x0,-0x78(%rbp)
    1750:	00 
    1751:	48 c7 45 90 00 00 00 	movq   $0x0,-0x70(%rbp)
    1758:	00 
    1759:	48 c7 45 98 00 00 00 	movq   $0x0,-0x68(%rbp)
    1760:	00 
    1761:	48 c7 45 a0 00 00 00 	movq   $0x0,-0x60(%rbp)
    1768:	00 
    1769:	48 c7 45 a8 00 00 00 	movq   $0x0,-0x58(%rbp)
    1770:	00 
    1771:	48 c7 45 b0 00 00 00 	movq   $0x0,-0x50(%rbp)
    1778:	00 
    1779:	48 c7 45 b8 00 00 00 	movq   $0x0,-0x48(%rbp)
    1780:	00 
    1781:	48 c7 45 c0 00 00 00 	movq   $0x0,-0x40(%rbp)
    1788:	00 
    1789:	48 c7 45 c8 00 00 00 	movq   $0x0,-0x38(%rbp)
    1790:	00 
    1791:	48 c7 45 d0 00 00 00 	movq   $0x0,-0x30(%rbp)
    1798:	00 
    1799:	48 c7 45 d8 00 00 00 	movq   $0x0,-0x28(%rbp)
    17a0:	00 
    17a1:	48 c7 45 e0 00 00 00 	movq   $0x0,-0x20(%rbp)
    17a8:	00 
    17a9:	48 c7 45 e8 00 00 00 	movq   $0x0,-0x18(%rbp)
    17b0:	00 
    17b1:	48 8d 95 f0 f4 ff ff 	lea    -0xb10(%rbp),%rdx
    17b8:	b8 00 00 00 00       	mov    $0x0,%eax
    17bd:	b9 00 01 00 00       	mov    $0x100,%ecx
    17c2:	48 89 d7             	mov    %rdx,%rdi
    17c5:	f3 48 ab             	rep stos %rax,%es:(%rdi)
    17c8:	e8 a3 fa ff ff       	call   1270 <getppid@plt>
    17cd:	89 c2                	mov    %eax,%edx
    17cf:	48 8d 85 f0 fd ff ff 	lea    -0x210(%rbp),%rax
    17d6:	89 d1                	mov    %edx,%ecx
    17d8:	48 8d 15 29 08 00 00 	lea    0x829(%rip),%rdx        # 2008 <_IO_stdin_used+0x8>
    17df:	be 00 01 00 00       	mov    $0x100,%esi
    17e4:	48 89 c7             	mov    %rax,%rdi
    17e7:	b8 00 00 00 00       	mov    $0x0,%eax
    17ec:	e8 ef f9 ff ff       	call   11e0 <snprintf@plt>
    17f1:	48 8d 8d f0 fe ff ff 	lea    -0x110(%rbp),%rcx
    17f8:	48 8d 85 f0 fd ff ff 	lea    -0x210(%rbp),%rax
    17ff:	ba ff 00 00 00       	mov    $0xff,%edx
    1804:	48 89 ce             	mov    %rcx,%rsi
    1807:	48 89 c7             	mov    %rax,%rdi
    180a:	e8 91 f9 ff ff       	call   11a0 <readlink@plt>
    180f:	48 8d 85 f0 fe ff ff 	lea    -0x110(%rbp),%rax
    1816:	48 8d 35 f8 07 00 00 	lea    0x7f8(%rip),%rsi        # 2015 <_IO_stdin_used+0x15>
    181d:	48 89 c7             	mov    %rax,%rdi
    1820:	e8 0b fa ff ff       	call   1230 <strcmp@plt>
    1825:	85 c0                	test   %eax,%eax
    1827:	0f 84 2d 01 00 00    	je     195a <auto_gdb+0x5d1>
    182d:	48 8d 85 f0 fc ff ff 	lea    -0x310(%rbp),%rax
    1834:	ba ff 00 00 00       	mov    $0xff,%edx
    1839:	48 89 c6             	mov    %rax,%rsi
    183c:	48 8d 3d df 07 00 00 	lea    0x7df(%rip),%rdi        # 2022 <_IO_stdin_used+0x22>
    1843:	e8 58 f9 ff ff       	call   11a0 <readlink@plt>
    1848:	48 8d 05 c6 07 00 00 	lea    0x7c6(%rip),%rax        # 2015 <_IO_stdin_used+0x15>
    184f:	48 89 85 f0 f4 ff ff 	mov    %rax,-0xb10(%rbp)
    1856:	81 bd dc f4 ff ff f9 	cmpl   $0xf9,-0xb24(%rbp)
    185d:	00 00 00 
    1860:	7e 1f                	jle    1881 <auto_gdb+0x4f8>
    1862:	48 8d 0d df 10 00 00 	lea    0x10df(%rip),%rcx        # 2948 <__PRETTY_FUNCTION__.5329>
    1869:	ba 26 00 00 00       	mov    $0x26,%edx
    186e:	48 8d 35 bc 07 00 00 	lea    0x7bc(%rip),%rsi        # 2031 <_IO_stdin_used+0x31>
    1875:	48 8d 3d bd 07 00 00 	lea    0x7bd(%rip),%rdi        # 2039 <_IO_stdin_used+0x39>
    187c:	e8 6f f9 ff ff       	call   11f0 <__assert_fail@plt>
    1881:	c7 85 ec f4 ff ff 01 	movl   $0x1,-0xb14(%rbp)
    1888:	00 00 00 
    188b:	eb 34                	jmp    18c1 <auto_gdb+0x538>
    188d:	8b 85 ec f4 ff ff    	mov    -0xb14(%rbp),%eax
    1893:	48 98                	cltq   
    1895:	48 8d 14 c5 00 00 00 	lea    0x0(,%rax,8),%rdx
    189c:	00 
    189d:	48 8b 85 d0 f4 ff ff 	mov    -0xb30(%rbp),%rax
    18a4:	48 01 d0             	add    %rdx,%rax
    18a7:	48 8b 10             	mov    (%rax),%rdx
    18aa:	8b 85 ec f4 ff ff    	mov    -0xb14(%rbp),%eax
    18b0:	48 98                	cltq   
    18b2:	48 89 94 c5 f0 f4 ff 	mov    %rdx,-0xb10(%rbp,%rax,8)
    18b9:	ff 
    18ba:	83 85 ec f4 ff ff 01 	addl   $0x1,-0xb14(%rbp)
    18c1:	8b 85 ec f4 ff ff    	mov    -0xb14(%rbp),%eax
    18c7:	3b 85 dc f4 ff ff    	cmp    -0xb24(%rbp),%eax
    18cd:	7c be                	jl     188d <auto_gdb+0x504>
    18cf:	8b 85 ec f4 ff ff    	mov    -0xb14(%rbp),%eax
    18d5:	8d 50 01             	lea    0x1(%rax),%edx
    18d8:	89 95 ec f4 ff ff    	mov    %edx,-0xb14(%rbp)
    18de:	48 98                	cltq   
    18e0:	48 8d 15 5d 07 00 00 	lea    0x75d(%rip),%rdx        # 2044 <_IO_stdin_used+0x44>
    18e7:	48 89 94 c5 f0 f4 ff 	mov    %rdx,-0xb10(%rbp,%rax,8)
    18ee:	ff 
    18ef:	8b 85 ec f4 ff ff    	mov    -0xb14(%rbp),%eax
    18f5:	8d 50 01             	lea    0x1(%rax),%edx
    18f8:	89 95 ec f4 ff ff    	mov    %edx,-0xb14(%rbp)
    18fe:	48 98                	cltq   
    1900:	48 8d 95 f0 fc ff ff 	lea    -0x310(%rbp),%rdx
    1907:	48 89 94 c5 f0 f4 ff 	mov    %rdx,-0xb10(%rbp,%rax,8)
    190e:	ff 
    190f:	8b 85 ec f4 ff ff    	mov    -0xb14(%rbp),%eax
    1915:	8d 50 01             	lea    0x1(%rax),%edx
    1918:	89 95 ec f4 ff ff    	mov    %edx,-0xb14(%rbp)
    191e:	48 98                	cltq   
    1920:	48 c7 84 c5 f0 f4 ff 	movq   $0x0,-0xb10(%rbp,%rax,8)
    1927:	ff 00 00 00 00 
    192c:	48 8d 3d 1d 07 00 00 	lea    0x71d(%rip),%rdi        # 2050 <_IO_stdin_used+0x50>
    1933:	e8 58 f8 ff ff       	call   1190 <puts@plt>
    1938:	48 8b 85 f0 f4 ff ff 	mov    -0xb10(%rbp),%rax
    193f:	48 8b 95 c8 f4 ff ff 	mov    -0xb38(%rbp),%rdx
    1946:	48 8d 8d f0 f4 ff ff 	lea    -0xb10(%rbp),%rcx
    194d:	48 89 ce             	mov    %rcx,%rsi
    1950:	48 89 c7             	mov    %rax,%rdi
    1953:	e8 c8 f8 ff ff       	call   1220 <execve@plt>
    1958:	eb 01                	jmp    195b <auto_gdb+0x5d2>
    195a:	90                   	nop
    195b:	48 8b 45 f8          	mov    -0x8(%rbp),%rax
    195f:	64 48 33 04 25 28 00 	xor    %fs:0x28,%rax
    1966:	00 00 
    1968:	74 05                	je     196f <auto_gdb+0x5e6>
    196a:	e8 51 f8 ff ff       	call   11c0 <__stack_chk_fail@plt>
    196f:	c9                   	leave  
    1970:	c3                   	ret    

0000000000001971 <breakpoint>:
    1971:	f3 0f 1e fa          	endbr64 
    1975:	55                   	push   %rbp
    1976:	48 89 e5             	mov    %rsp,%rbp
    1979:	cc                   	int3   
    197a:	90                   	nop
    197b:	5d                   	pop    %rbp
    197c:	c3                   	ret    

000000000000197d <win>:
    197d:	f3 0f 1e fa          	endbr64 
    1981:	55                   	push   %rbp
    1982:	48 89 e5             	mov    %rsp,%rbp
    1985:	48 81 ec 20 01 00 00 	sub    $0x120,%rsp
    198c:	64 48 8b 04 25 28 00 	mov    %fs:0x28,%rax
    1993:	00 00 
    1995:	48 89 45 f8          	mov    %rax,-0x8(%rbp)
    1999:	31 c0                	xor    %eax,%eax
    199b:	48 8d 3d 17 07 00 00 	lea    0x717(%rip),%rdi        # 20b9 <_IO_stdin_used+0xb9>
    19a2:	e8 e9 f7 ff ff       	call   1190 <puts@plt>
    19a7:	be 00 00 00 00       	mov    $0x0,%esi
    19ac:	48 8d 3d 22 07 00 00 	lea    0x722(%rip),%rdi        # 20d5 <_IO_stdin_used+0xd5>
    19b3:	b8 00 00 00 00       	mov    $0x0,%eax
    19b8:	e8 93 f8 ff ff       	call   1250 <open@plt>
    19bd:	89 85 e8 fe ff ff    	mov    %eax,-0x118(%rbp)
    19c3:	83 bd e8 fe ff ff 00 	cmpl   $0x0,-0x118(%rbp)
    19ca:	79 49                	jns    1a15 <win+0x98>
    19cc:	e8 af f7 ff ff       	call   1180 <__errno_location@plt>
    19d1:	8b 00                	mov    (%rax),%eax
    19d3:	89 c7                	mov    %eax,%edi
    19d5:	e8 b6 f8 ff ff       	call   1290 <strerror@plt>
    19da:	48 89 c6             	mov    %rax,%rsi
    19dd:	48 8d 3d fc 06 00 00 	lea    0x6fc(%rip),%rdi        # 20e0 <_IO_stdin_used+0xe0>
    19e4:	b8 00 00 00 00       	mov    $0x0,%eax
    19e9:	e8 e2 f7 ff ff       	call   11d0 <printf@plt>
    19ee:	e8 0d f8 ff ff       	call   1200 <geteuid@plt>
    19f3:	85 c0                	test   %eax,%eax
    19f5:	0f 84 94 00 00 00    	je     1a8f <win+0x112>
    19fb:	48 8d 3d 0e 07 00 00 	lea    0x70e(%rip),%rdi        # 2110 <_IO_stdin_used+0x110>
    1a02:	e8 89 f7 ff ff       	call   1190 <puts@plt>
    1a07:	48 8d 3d 2a 07 00 00 	lea    0x72a(%rip),%rdi        # 2138 <_IO_stdin_used+0x138>
    1a0e:	e8 7d f7 ff ff       	call   1190 <puts@plt>
    1a13:	eb 7a                	jmp    1a8f <win+0x112>
    1a15:	48 8d 8d f0 fe ff ff 	lea    -0x110(%rbp),%rcx
    1a1c:	8b 85 e8 fe ff ff    	mov    -0x118(%rbp),%eax
    1a22:	ba 00 01 00 00       	mov    $0x100,%edx
    1a27:	48 89 ce             	mov    %rcx,%rsi
    1a2a:	89 c7                	mov    %eax,%edi
    1a2c:	e8 df f7 ff ff       	call   1210 <read@plt>
    1a31:	89 85 ec fe ff ff    	mov    %eax,-0x114(%rbp)
    1a37:	83 bd ec fe ff ff 00 	cmpl   $0x0,-0x114(%rbp)
    1a3e:	7f 24                	jg     1a64 <win+0xe7>
    1a40:	e8 3b f7 ff ff       	call   1180 <__errno_location@plt>
    1a45:	8b 00                	mov    (%rax),%eax
    1a47:	89 c7                	mov    %eax,%edi
    1a49:	e8 42 f8 ff ff       	call   1290 <strerror@plt>
    1a4e:	48 89 c6             	mov    %rax,%rsi
    1a51:	48 8d 3d 38 07 00 00 	lea    0x738(%rip),%rdi        # 2190 <_IO_stdin_used+0x190>
    1a58:	b8 00 00 00 00       	mov    $0x0,%eax
    1a5d:	e8 6e f7 ff ff       	call   11d0 <printf@plt>
    1a62:	eb 2c                	jmp    1a90 <win+0x113>
    1a64:	8b 85 ec fe ff ff    	mov    -0x114(%rbp),%eax
    1a6a:	48 63 d0             	movslq %eax,%rdx
    1a6d:	48 8d 85 f0 fe ff ff 	lea    -0x110(%rbp),%rax
    1a74:	48 89 c6             	mov    %rax,%rsi
    1a77:	bf 01 00 00 00       	mov    $0x1,%edi
    1a7c:	e8 2f f7 ff ff       	call   11b0 <write@plt>
    1a81:	48 8d 3d 32 07 00 00 	lea    0x732(%rip),%rdi        # 21ba <_IO_stdin_used+0x1ba>
    1a88:	e8 03 f7 ff ff       	call   1190 <puts@plt>
    1a8d:	eb 01                	jmp    1a90 <win+0x113>
    1a8f:	90                   	nop
    1a90:	48 8b 45 f8          	mov    -0x8(%rbp),%rax
    1a94:	64 48 33 04 25 28 00 	xor    %fs:0x28,%rax
    1a9b:	00 00 
    1a9d:	74 05                	je     1aa4 <win+0x127>
    1a9f:	e8 1c f7 ff ff       	call   11c0 <__stack_chk_fail@plt>
    1aa4:	c9                   	leave  
    1aa5:	c3                   	ret    

0000000000001aa6 <main>:
    1aa6:	f3 0f 1e fa          	endbr64 
    1aaa:	55                   	push   %rbp
    1aab:	48 89 e5             	mov    %rsp,%rbp
    1aae:	48 83 ec 40          	sub    $0x40,%rsp
    1ab2:	89 7d dc             	mov    %edi,-0x24(%rbp)
    1ab5:	48 89 75 d0          	mov    %rsi,-0x30(%rbp)
    1ab9:	48 89 55 c8          	mov    %rdx,-0x38(%rbp)
    1abd:	64 48 8b 04 25 28 00 	mov    %fs:0x28,%rax
    1ac4:	00 00 
    1ac6:	48 89 45 f8          	mov    %rax,-0x8(%rbp)
    1aca:	31 c0                	xor    %eax,%eax
    1acc:	83 7d dc 00          	cmpl   $0x0,-0x24(%rbp)
    1ad0:	7f 1f                	jg     1af1 <main+0x4b>
    1ad2:	48 8d 0d 78 0e 00 00 	lea    0xe78(%rip),%rcx        # 2951 <__PRETTY_FUNCTION__.5345>
    1ad9:	ba 51 00 00 00       	mov    $0x51,%edx
    1ade:	48 8d 35 4c 05 00 00 	lea    0x54c(%rip),%rsi        # 2031 <_IO_stdin_used+0x31>
    1ae5:	48 8d 3d d0 06 00 00 	lea    0x6d0(%rip),%rdi        # 21bc <_IO_stdin_used+0x1bc>
    1aec:	e8 ff f6 ff ff       	call   11f0 <__assert_fail@plt>
    1af1:	48 8d 3d cd 06 00 00 	lea    0x6cd(%rip),%rdi        # 21c5 <_IO_stdin_used+0x1c5>
    1af8:	e8 93 f6 ff ff       	call   1190 <puts@plt>
    1afd:	48 8b 45 d0          	mov    -0x30(%rbp),%rax
    1b01:	48 8b 00             	mov    (%rax),%rax
    1b04:	48 89 c6             	mov    %rax,%rsi
    1b07:	48 8d 3d bb 06 00 00 	lea    0x6bb(%rip),%rdi        # 21c9 <_IO_stdin_used+0x1c9>
    1b0e:	b8 00 00 00 00       	mov    $0x0,%eax
    1b13:	e8 b8 f6 ff ff       	call   11d0 <printf@plt>
    1b18:	48 8d 3d a6 06 00 00 	lea    0x6a6(%rip),%rdi        # 21c5 <_IO_stdin_used+0x1c5>
    1b1f:	e8 6c f6 ff ff       	call   1190 <puts@plt>
    1b24:	bf 0a 00 00 00       	mov    $0xa,%edi
    1b29:	e8 42 f6 ff ff       	call   1170 <putchar@plt>
    1b2e:	48 8b 05 eb 24 00 00 	mov    0x24eb(%rip),%rax        # 4020 <stdin@GLIBC_2.2.5>
    1b35:	b9 00 00 00 00       	mov    $0x0,%ecx
    1b3a:	ba 02 00 00 00       	mov    $0x2,%edx
    1b3f:	be 00 00 00 00       	mov    $0x0,%esi
    1b44:	48 89 c7             	mov    %rax,%rdi
    1b47:	e8 f4 f6 ff ff       	call   1240 <setvbuf@plt>
    1b4c:	48 8b 05 bd 24 00 00 	mov    0x24bd(%rip),%rax        # 4010 <stdout@GLIBC_2.2.5>
    1b53:	b9 01 00 00 00       	mov    $0x1,%ecx
    1b58:	ba 02 00 00 00       	mov    $0x2,%edx
    1b5d:	be 00 00 00 00       	mov    $0x0,%esi
    1b62:	48 89 c7             	mov    %rax,%rdi
    1b65:	e8 d6 f6 ff ff       	call   1240 <setvbuf@plt>
    1b6a:	48 8d 3d 6f 06 00 00 	lea    0x66f(%rip),%rdi        # 21e0 <_IO_stdin_used+0x1e0>
    1b71:	e8 1a f6 ff ff       	call   1190 <puts@plt>
    1b76:	48 8d 3d db 06 00 00 	lea    0x6db(%rip),%rdi        # 2258 <_IO_stdin_used+0x258>
    1b7d:	e8 0e f6 ff ff       	call   1190 <puts@plt>
    1b82:	48 8d 3d 2f 07 00 00 	lea    0x72f(%rip),%rdi        # 22b8 <_IO_stdin_used+0x2b8>
    1b89:	e8 02 f6 ff ff       	call   1190 <puts@plt>
    1b8e:	48 8d 3d a3 07 00 00 	lea    0x7a3(%rip),%rdi        # 2338 <_IO_stdin_used+0x338>
    1b95:	e8 f6 f5 ff ff       	call   1190 <puts@plt>
    1b9a:	48 8d 3d 17 08 00 00 	lea    0x817(%rip),%rdi        # 23b8 <_IO_stdin_used+0x3b8>
    1ba1:	e8 ea f5 ff ff       	call   1190 <puts@plt>
    1ba6:	48 8d 3d 73 08 00 00 	lea    0x873(%rip),%rdi        # 2420 <_IO_stdin_used+0x420>
    1bad:	e8 de f5 ff ff       	call   1190 <puts@plt>
    1bb2:	48 8d 3d df 08 00 00 	lea    0x8df(%rip),%rdi        # 2498 <_IO_stdin_used+0x498>
    1bb9:	e8 d2 f5 ff ff       	call   1190 <puts@plt>
    1bbe:	48 8d 3d 4b 09 00 00 	lea    0x94b(%rip),%rdi        # 2510 <_IO_stdin_used+0x510>
    1bc5:	e8 c6 f5 ff ff       	call   1190 <puts@plt>
    1bca:	48 8d 3d 97 09 00 00 	lea    0x997(%rip),%rdi        # 2568 <_IO_stdin_used+0x568>
    1bd1:	e8 ba f5 ff ff       	call   1190 <puts@plt>
    1bd6:	48 8d 3d 03 0a 00 00 	lea    0xa03(%rip),%rdi        # 25e0 <_IO_stdin_used+0x5e0>
    1bdd:	e8 ae f5 ff ff       	call   1190 <puts@plt>
    1be2:	48 8d 3d 6b 0a 00 00 	lea    0xa6b(%rip),%rdi        # 2654 <_IO_stdin_used+0x654>
    1be9:	e8 a2 f5 ff ff       	call   1190 <puts@plt>
    1bee:	48 8d 3d 76 0a 00 00 	lea    0xa76(%rip),%rdi        # 266b <_IO_stdin_used+0x66b>
    1bf5:	e8 96 f5 ff ff       	call   1190 <puts@plt>
    1bfa:	48 8d 3d 72 0a 00 00 	lea    0xa72(%rip),%rdi        # 2673 <_IO_stdin_used+0x673>
    1c01:	e8 8a f5 ff ff       	call   1190 <puts@plt>
    1c06:	48 8d 3d 7b 0a 00 00 	lea    0xa7b(%rip),%rdi        # 2688 <_IO_stdin_used+0x688>
    1c0d:	e8 7e f5 ff ff       	call   1190 <puts@plt>
    1c12:	48 8d 3d 7a 0a 00 00 	lea    0xa7a(%rip),%rdi        # 2693 <_IO_stdin_used+0x693>
    1c19:	e8 72 f5 ff ff       	call   1190 <puts@plt>
    1c1e:	48 8d 3d 79 0a 00 00 	lea    0xa79(%rip),%rdi        # 269e <_IO_stdin_used+0x69e>
    1c25:	e8 66 f5 ff ff       	call   1190 <puts@plt>
    1c2a:	48 8d 3d 81 0a 00 00 	lea    0xa81(%rip),%rdi        # 26b2 <_IO_stdin_used+0x6b2>
    1c31:	e8 5a f5 ff ff       	call   1190 <puts@plt>
    1c36:	48 8d 3d 88 0a 00 00 	lea    0xa88(%rip),%rdi        # 26c5 <_IO_stdin_used+0x6c5>
    1c3d:	e8 4e f5 ff ff       	call   1190 <puts@plt>
    1c42:	48 8d 3d 84 0a 00 00 	lea    0xa84(%rip),%rdi        # 26cd <_IO_stdin_used+0x6cd>
    1c49:	e8 42 f5 ff ff       	call   1190 <puts@plt>
    1c4e:	48 8d 3d 85 0a 00 00 	lea    0xa85(%rip),%rdi        # 26da <_IO_stdin_used+0x6da>
    1c55:	e8 36 f5 ff ff       	call   1190 <puts@plt>
    1c5a:	48 8d 3d 7f 0a 00 00 	lea    0xa7f(%rip),%rdi        # 26e0 <_IO_stdin_used+0x6e0>
    1c61:	e8 2a f5 ff ff       	call   1190 <puts@plt>
    1c66:	48 8d 3d 7e 0a 00 00 	lea    0xa7e(%rip),%rdi        # 26eb <_IO_stdin_used+0x6eb>
    1c6d:	e8 1e f5 ff ff       	call   1190 <puts@plt>
    1c72:	48 8d 3d 77 0a 00 00 	lea    0xa77(%rip),%rdi        # 26f0 <_IO_stdin_used+0x6f0>
    1c79:	e8 12 f5 ff ff       	call   1190 <puts@plt>
    1c7e:	48 8d 3d e3 0a 00 00 	lea    0xae3(%rip),%rdi        # 2768 <_IO_stdin_used+0x768>
    1c85:	e8 06 f5 ff ff       	call   1190 <puts@plt>
    1c8a:	48 8d 3d 17 0b 00 00 	lea    0xb17(%rip),%rdi        # 27a8 <_IO_stdin_used+0x7a8>
    1c91:	e8 fa f4 ff ff       	call   1190 <puts@plt>
    1c96:	48 8d 3d 83 0b 00 00 	lea    0xb83(%rip),%rdi        # 2820 <_IO_stdin_used+0x820>
    1c9d:	e8 ee f4 ff ff       	call   1190 <puts@plt>
    1ca2:	48 8d 3d ef 0b 00 00 	lea    0xbef(%rip),%rdi        # 2898 <_IO_stdin_used+0x898>
    1ca9:	e8 e2 f4 ff ff       	call   1190 <puts@plt>
    1cae:	cc                   	int3   
    1caf:	90                   	nop
    1cb0:	c7 45 e4 00 00 00 00 	movl   $0x0,-0x1c(%rbp)
    1cb7:	e9 ab 00 00 00       	jmp    1d67 <main+0x2c1>
    1cbc:	be 00 00 00 00       	mov    $0x0,%esi
    1cc1:	48 8d 3d 06 0c 00 00 	lea    0xc06(%rip),%rdi        # 28ce <_IO_stdin_used+0x8ce>
    1cc8:	b8 00 00 00 00       	mov    $0x0,%eax
    1ccd:	e8 7e f5 ff ff       	call   1250 <open@plt>
    1cd2:	89 c1                	mov    %eax,%ecx
    1cd4:	48 8d 45 e8          	lea    -0x18(%rbp),%rax
    1cd8:	ba 08 00 00 00       	mov    $0x8,%edx
    1cdd:	48 89 c6             	mov    %rax,%rsi
    1ce0:	89 cf                	mov    %ecx,%edi
    1ce2:	e8 29 f5 ff ff       	call   1210 <read@plt>
    1ce7:	48 8d 3d f2 0b 00 00 	lea    0xbf2(%rip),%rdi        # 28e0 <_IO_stdin_used+0x8e0>
    1cee:	e8 9d f4 ff ff       	call   1190 <puts@plt>
    1cf3:	48 8d 3d 06 0c 00 00 	lea    0xc06(%rip),%rdi        # 2900 <_IO_stdin_used+0x900>
    1cfa:	b8 00 00 00 00       	mov    $0x0,%eax
    1cff:	e8 cc f4 ff ff       	call   11d0 <printf@plt>
    1d04:	48 8d 45 f0          	lea    -0x10(%rbp),%rax
    1d08:	48 89 c6             	mov    %rax,%rsi
    1d0b:	48 8d 3d fd 0b 00 00 	lea    0xbfd(%rip),%rdi        # 290f <_IO_stdin_used+0x90f>
    1d12:	b8 00 00 00 00       	mov    $0x0,%eax
    1d17:	e8 44 f5 ff ff       	call   1260 <__isoc99_scanf@plt>
    1d1c:	48 8b 45 f0          	mov    -0x10(%rbp),%rax
    1d20:	48 89 c6             	mov    %rax,%rsi
    1d23:	48 8d 3d ea 0b 00 00 	lea    0xbea(%rip),%rdi        # 2914 <_IO_stdin_used+0x914>
    1d2a:	b8 00 00 00 00       	mov    $0x0,%eax
    1d2f:	e8 9c f4 ff ff       	call   11d0 <printf@plt>
    1d34:	48 8b 45 e8          	mov    -0x18(%rbp),%rax
    1d38:	48 89 c6             	mov    %rax,%rsi
    1d3b:	48 8d 3d e3 0b 00 00 	lea    0xbe3(%rip),%rdi        # 2925 <_IO_stdin_used+0x925>
    1d42:	b8 00 00 00 00       	mov    $0x0,%eax
    1d47:	e8 84 f4 ff ff       	call   11d0 <printf@plt>
    1d4c:	48 8b 55 f0          	mov    -0x10(%rbp),%rdx
    1d50:	48 8b 45 e8          	mov    -0x18(%rbp),%rax
    1d54:	48 39 c2             	cmp    %rax,%rdx
    1d57:	74 0a                	je     1d63 <main+0x2bd>
    1d59:	bf 01 00 00 00       	mov    $0x1,%edi
    1d5e:	e8 1d f5 ff ff       	call   1280 <exit@plt>
    1d63:	83 45 e4 01          	addl   $0x1,-0x1c(%rbp)
    1d67:	83 7d e4 3f          	cmpl   $0x3f,-0x1c(%rbp)
    1d6b:	0f 8e 4b ff ff ff    	jle    1cbc <main+0x216>
    1d71:	b8 00 00 00 00       	mov    $0x0,%eax
    1d76:	e8 02 fc ff ff       	call   197d <win>
    1d7b:	b8 00 00 00 00       	mov    $0x0,%eax
    1d80:	48 8b 4d f8          	mov    -0x8(%rbp),%rcx
    1d84:	64 48 33 0c 25 28 00 	xor    %fs:0x28,%rcx
    1d8b:	00 00 
    1d8d:	74 05                	je     1d94 <main+0x2ee>
    1d8f:	e8 2c f4 ff ff       	call   11c0 <__stack_chk_fail@plt>
    1d94:	c9                   	leave  
    1d95:	c3                   	ret    
    1d96:	66 2e 0f 1f 84 00 00 	cs nopw 0x0(%rax,%rax,1)
    1d9d:	00 00 00 

0000000000001da0 <__libc_csu_init>:
    1da0:	f3 0f 1e fa          	endbr64 
    1da4:	41 57                	push   %r15
    1da6:	4c 8d 3d 73 1f 00 00 	lea    0x1f73(%rip),%r15        # 3d20 <__frame_dummy_init_array_entry>
    1dad:	41 56                	push   %r14
    1daf:	49 89 d6             	mov    %rdx,%r14
    1db2:	41 55                	push   %r13
    1db4:	49 89 f5             	mov    %rsi,%r13
    1db7:	41 54                	push   %r12
    1db9:	41 89 fc             	mov    %edi,%r12d
    1dbc:	55                   	push   %rbp
    1dbd:	48 8d 2d 6c 1f 00 00 	lea    0x1f6c(%rip),%rbp        # 3d30 <__do_global_dtors_aux_fini_array_entry>
    1dc4:	53                   	push   %rbx
    1dc5:	4c 29 fd             	sub    %r15,%rbp
    1dc8:	48 83 ec 08          	sub    $0x8,%rsp
    1dcc:	e8 2f f2 ff ff       	call   1000 <_init>
    1dd1:	48 c1 fd 03          	sar    $0x3,%rbp
    1dd5:	74 1f                	je     1df6 <__libc_csu_init+0x56>
    1dd7:	31 db                	xor    %ebx,%ebx
    1dd9:	0f 1f 80 00 00 00 00 	nopl   0x0(%rax)
    1de0:	4c 89 f2             	mov    %r14,%rdx
    1de3:	4c 89 ee             	mov    %r13,%rsi
    1de6:	44 89 e7             	mov    %r12d,%edi
    1de9:	41 ff 14 df          	call   *(%r15,%rbx,8)
    1ded:	48 83 c3 01          	add    $0x1,%rbx
    1df1:	48 39 dd             	cmp    %rbx,%rbp
    1df4:	75 ea                	jne    1de0 <__libc_csu_init+0x40>
    1df6:	48 83 c4 08          	add    $0x8,%rsp
    1dfa:	5b                   	pop    %rbx
    1dfb:	5d                   	pop    %rbp
    1dfc:	41 5c                	pop    %r12
    1dfe:	41 5d                	pop    %r13
    1e00:	41 5e                	pop    %r14
    1e02:	41 5f                	pop    %r15
    1e04:	c3                   	ret    
    1e05:	66 66 2e 0f 1f 84 00 	data16 cs nopw 0x0(%rax,%rax,1)
    1e0c:	00 00 00 00 

0000000000001e10 <__libc_csu_fini>:
    1e10:	f3 0f 1e fa          	endbr64 
    1e14:	c3                   	ret    

Disassembly of section .fini:

0000000000001e18 <_fini>:
    1e18:	f3 0f 1e fa          	endbr64 
    1e1c:	48 83 ec 08          	sub    $0x8,%rsp
    1e20:	48 83 c4 08          	add    $0x8,%rsp
    1e24:	c3                   	ret    
