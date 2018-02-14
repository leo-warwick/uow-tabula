package uk.ac.warwick.tabula.services.timetables

import uk.ac.warwick.tabula.data.model.MapLocation


object ScientiaCentrallyManagedRooms {

	// Big ugly hardcoded map of all the rooms in the timetabling system
	// If you have been asked to rename any rooms because the owning department has changed (or for any other reason really) please let Russell Boyatt know

	final val CentrallyManagedRooms: Map[String, MapLocation] = Map(
		"IB_2.011" -> MapLocation("2.011", "25555", Some("IB_2.011")),
		"FAC.ART_H4.54" -> MapLocation("H4.54", "27050", Some("FAC.ART_H4.54")),
		"HI_H3.47" -> MapLocation("H3.47", "27039", Some("HI_H3.47")),
		"FAC.ART_H4.55" -> MapLocation("H4.55", "27051", Some("FAC.ART_H4.55")),
		"ET_S1.88" -> MapLocation("S1.88", "37954", Some("ET_S1.88")),
		"ES_F5.06" -> MapLocation("F5.06", "31379", Some("ES_F5.06")),
		"ES_IMC" -> MapLocation("International Manufacturing Centre", "23988", Some("ES_IMC")),
		"H5.45" -> MapLocation("H5.45", "21777", Some("H5.45")),
		"ES_D2.12" -> MapLocation("D2.12", "51315", Some("ES_D2.12")),
		"BS_E110" -> MapLocation("Teaching Lab 2", "35374", Some("BS_E110")),
		"HA_F25B" -> MapLocation("F25b", "28567", Some("HA_F25B")),
		"IB_3.003" -> MapLocation("3.003", "51589", Some("IB_3.003")),
		"IB_2.006" -> MapLocation("2.006", "51541", Some("IB_2.006")),
		"IB_2.005" -> MapLocation("2.005", "51542", Some("IB_2.005")),
		"IB_0.011" -> MapLocation("0.011", "33925", Some("IB_0.011")),
		"IB_0.009" -> MapLocation("0.009", "33926", Some("IB_0.009")),
		"IB_0.103" -> MapLocation("0.103", "33937", Some("IB_0.103")),
		"IB_1.015" -> MapLocation("1.015", "25471", Some("IB_1.015")),
		"IB_0.102" -> MapLocation("0.102", "33977", Some("IB_0.102")),
		"IB_2.004" -> MapLocation("2.004", "51528", Some("IB_2.004")),
		"IB_2.003" -> MapLocation("2.003", "51521", Some("IB_2.003")),
		"IB_3.006" -> MapLocation("3.006", "25692", Some("IB_3.006")),
		"IB_1.009" -> MapLocation("1.009", "25464", Some("IB_1.009")),
		"IB_0.006" -> MapLocation("0.006", "33929", Some("IB_0.006")),
		"MA_B1.01" -> MapLocation("B1.01", "41202", Some("MA_B1.01")),
		"FAC.SS_S2.77" -> MapLocation("S2.77", "37695", Some("FAC.SS_S2.77")),
		"ET_A1.11" -> MapLocation("A1.11", "38185", Some("ET_A1.11")),
		"ES_F1.05" -> MapLocation("F1.05", "30927", Some("ES_F1.05")),
		"HI_H3.45" -> MapLocation("H3.45", "21598", Some("HI_H3.45")),
		"WM_0.04" -> MapLocation("IMC0.04", "19673", Some("WM_0.04")),
		"MD_MSBB0.26" -> MapLocation("B0.26", "36999", Some("MD_MSBB0.26")),
		"PO_B0.06" -> MapLocation("B0.06", "37576", Some("PO_B0.06")),
		"PS_H1.48a" -> MapLocation("H1.48a", "26967", Some("PS_H1.48a")),
		"IL_H0.76" -> MapLocation("Humanities Studio", "21375", Some("IL_H0.76")),
		"WM_1.06" -> MapLocation("IMC1.06", "46063", Some("WM_1.06")),
		"TH_G.51" -> MapLocation("Scene Dock", "28504", Some("TH_G.51")),
		"CH_LABS4" -> MapLocation("B4.07", "44912", Some("CH_LABS4")),
		"CH_LABS3" -> MapLocation("B3.08", "44802", Some("CH_LABS3")),
		"FI_A1.27" -> MapLocation("A1.27", "28586", Some("FI_A1.27")),
		"F.25B (Millburn)" -> MapLocation("F25b", "28567", Some("F.25B (Millburn)")),
		"A0.26 (Millburn)" -> MapLocation("A0.26", "28459", Some("A0.26 (Millburn)")),
		"A1.25 (Millburn)" -> MapLocation("A1.25", "28587", Some("A1.25 (Millburn)")),
		"PX_P5.23" -> MapLocation("P5.23", "35460", Some("PX_P5.23")),
		"IL_R0.12" -> MapLocation("R0.12", "45706", Some("IL_R0.12")),
		"ST_C1.06" -> MapLocation("C1.06", "41240", Some("ST_C1.06")),
		"PX_P3.45" -> MapLocation("M Phys Laboratory", "35574", Some("PX_P3.45")),
		"IB_M2" -> MapLocation("M2", "29373", Some("IB_M2")),
		"A0.28 (Millburn)" -> MapLocation("A0.28", "28461", Some("A0.28 (Millburn)")),
		"CH_C5.23" -> MapLocation("Chemistry Common Room", "44977", Some("CH_C5.23")),
		"H5.22" -> MapLocation("H5.22", "21718", Some("H5.22")),
		"EN_H5.07" -> MapLocation("H5.07", "21761", Some("EN_H5.07")),
		"EN_H5.43" -> MapLocation("H5.43", "21725", Some("EN_H5.43")),
		"MD_MTC1.04" -> MapLocation("C1.04", "37247", Some("MD_MTC1.04")),
		"MD_MTC1.05" -> MapLocation("C1.05", "37251", Some("MD_MTC1.05")),
		"MD_MTC1.06" -> MapLocation("C1.06", "37246", Some("MD_MTC1.06")),
		"MD0.01" -> MapLocation("MD0.01", "20674", Some("MD0.01")),
		"FI_A0.26" -> MapLocation("A0.26", "28459", Some("FI_A0.26")),
		"A1.28 (Millburn)" -> MapLocation("A1.28", "28585", Some("A1.28 (Millburn)")),
		"MS.01" -> MapLocation("MS.01", "40858", Some("MS.01")),
		"MS.05" -> MapLocation("MS.05", "29048", Some("MS.05")),
		"MS.02" -> MapLocation("MS.02", "40879", Some("MS.02")),
		"MS.04" -> MapLocation("MS.04", "29046", Some("MS.04")),
		"MS.03" -> MapLocation("MS.03", "29050", Some("MS.03")),
		"LA_S1.14" -> MapLocation("S1.14", "38035", Some("LA_S1.14")),
		"ARTS-CINEMA" -> MapLocation("Cinema", "41920", Some("ARTS-CINEMA")),
		"P5.21" -> MapLocation("P5.21A", "35461", Some("P5.21")),
		"IMC0.02" -> MapLocation("IMC0.02", "19679", Some("IMC0.02")),
		"IN_A0.01 (PC room - Linux)" -> MapLocation("A0.01", "40793", Some("IN_A0.01 (PC room - Linux)")),
		"MAS_CONCOURSE" -> MapLocation("MAS Concourse", "33345", Some("MAS_CONCOURSE")), // TODO - Not a clickable room - set to AS2.01 location for the time being as it's the closest location
		"H3.05" -> MapLocation("H3.05", "21586", Some("H3.05")),
		"MAS_2.06" -> MapLocation("MAS2.06", "33353", Some("MAS_2.06")),
		"MAS_2.03" -> MapLocation("MAS2.03", "33347", Some("MAS_2.03")),
		"MAS_2.04" -> MapLocation("MAS2.04", "33349", Some("MAS_2.04")),
		"MAS_2.05" -> MapLocation("MAS2.05", "33352", Some("MAS_2.05")),
		"H0.01" -> MapLocation("H0.01", "21318", Some("H0.01")),
		"ES_A2.06" -> MapLocation("A2.06", "31004", Some("ES_A2.06")),
		"ES_F0.24" -> MapLocation("F0.24", "30680", Some("ES_F0.24")),
		"ES_F1.04" -> MapLocation("Lobby", "30926", Some("ES_F1.04")),
		"EC_S2.79" -> MapLocation("S2.79", "37696", Some("EC_S2.79")),
		"IN_A0.03 (PC room - Zeeman)" -> MapLocation("A0.03", "40810", Some("IN_A0.03 (PC room - Zeeman)")),
		"GLT3" -> MapLocation("GLT3", "36854", Some("GLT3")),
		"WT0.04" -> MapLocation("WT0.04", "38983", Some("WT0.04")),
		"WT0.02" -> MapLocation("WT0.02", "38986", Some("WT0.02")),
		"WT0.03" -> MapLocation("WT0.03", "38987", Some("WT0.03")),
		"WT0.05" -> MapLocation("WT0.05", "38982", Some("WT0.05")),
		"WT1.01" -> MapLocation("WT1.01", "38997", Some("WT1.01")),
		"WT1.04" -> MapLocation("WT1.04", "39005", Some("WT1.04")),
		"WT1.04/5" -> MapLocation("WT1.04", "39005", Some("WT1.04/5")),
		"WT1.05" -> MapLocation("WT1.05", "39004", Some("WT1.05")),
		"ES_D0.02" -> MapLocation("D0.02", "30952", Some("ES_D0.02")),
		"ES_F0.03" -> MapLocation("F0.03", "30673", Some("ES_F0.03")),
		"ES_A1.16" -> MapLocation("A1.16", "30969", Some("ES_A1.16")),
		"ES_F2.10" -> MapLocation("F2.10", "31017", Some("ES_F2.10")),
		"ES_A2.02" -> MapLocation("A2.02", "31012", Some("ES_A2.02")),
		"ES_A0.08" -> MapLocation("A0.08B", "30689", Some("ES_A0.08")),
		"MD_MSBA0.41" -> MapLocation("A0.41", "36945", Some("MD_MSBA0.41")),
		"MD_MSBA0.30" -> MapLocation("A0.30", "36946", Some("MD_MSBA0.30")),
		"MD_MSBA0.42" -> MapLocation("A0.42", "36904", Some("MD_MSBA0.42")),
		"MD_MSBA1.50" -> MapLocation("A1.50", "37173", Some("MD_MSBA1.50")),
		"H3.58" -> MapLocation("H3.58", "27042", Some("H3.58")),
		"PO_S1.50" -> MapLocation("S1.50", "37989", Some("PO_S1.50")),
		"PX_PS0.18" -> MapLocation("PS0.18", "27428", Some("PX_PS0.18")),
		"H2.46" -> MapLocation("H2.46", "21532", Some("H2.46")),
		"H3.02" -> MapLocation("H3.02", "21583", Some("H3.02")),
		"E0.23 (Soc Sci)" -> MapLocation("E0.23", "37501", Some("E0.23 (Soc Sci)")),
		"H4.03" -> MapLocation("H4.03", "21658", Some("H4.03")),
		"ES_F1.06" -> MapLocation("F1.06", "30928", Some("ES_F1.06")),
		"IL_REINV" -> MapLocation("Reinvention Centre", "47252", Some("IL_REINV")),
		"ES_F3.08" -> MapLocation("F3.08", "31197", Some("ES_F3.08")),
		"MA_B3.02" -> MapLocation("B3.02", "29096", Some("MA_B3.02")),
		"H1.03" -> MapLocation("H1.03", "21430", Some("H1.03")),
		"HI_H3.03" -> MapLocation("H3.03", "21584", Some("HI_H3.03")),
		"H0.05" -> MapLocation("H0.05/4", "21315", Some("H0.05")),
		"H2.44" -> MapLocation("H2.44", "21536", Some("H2.44")),
		"H0.02" -> MapLocation("H0.02", "21317", Some("H0.02")),
		"EN_H5.42" -> MapLocation("H5.42", "21721", Some("EN_H5.42")),
		"S0.17" -> MapLocation("S0.17", "37481", Some("S0.17")),
		"WOODS-SCAWEN" -> MapLocation("Woods-Scawen room", "42011", Some("WOODS-SCAWEN")),
		"L4" -> MapLocation("Lecture Theatre 4", "31390", Some("L4")),
		"S0.10" -> MapLocation("S0.10", "37490", Some("S0.10")),
		"S0.13" -> MapLocation("S0.13", "37476", Some("S0.13")),
		"S0.18" -> MapLocation("S0.18", "37482", Some("S0.18")),
		"S1.141" -> MapLocation("S1.141", "37910", Some("S1.141")),
		"S0.19" -> MapLocation("S0.19", "37483", Some("S0.19")),
		"GLT1" -> MapLocation("GLT1", "37286", Some("GLT1")),
		"H0.51" -> MapLocation("H0.51", "21336", Some("H0.51")),
		"H3.44" -> MapLocation("H3.44", "21601", Some("H3.44")),
		"WLT" -> MapLocation("WLT", "38986", Some("WLT")),
		"B2.04/5 (Sci Conc)" -> MapLocation("B2.04", "31395", Some("B2.04/5 (Sci Conc)")),
		"A0.05 (Soc Sci)" -> MapLocation("A0.05", "37626", Some("A0.05 (Soc Sci)")),
		"L3" -> MapLocation("L3", "31456", Some("L3")),
		"S0.28" -> MapLocation("S0.28", "37406", Some("S0.28")),
		"S0.20" -> MapLocation("S0.20", "37484", Some("S0.20")),
		"PLT" -> MapLocation("PLT", "35601", Some("PLT")),
		"S0.21" -> MapLocation("S0.21", "37486", Some("S0.21")),
		"S2.73" -> MapLocation("S2.73", "37727", Some("S2.73")),
		"H0.52" -> MapLocation("H0.52", "21337", Some("H0.52")),
		"H4.45" -> MapLocation("H4.45", "21670", Some("H4.45")),
		"H0.03" -> MapLocation("H0.03", "21316", Some("H0.03")),
		"GLT2" -> MapLocation("GLT2", "37284", Some("GLT2")),
		"S0.09" -> MapLocation("S0.09", "37491", Some("S0.09")),
		"H0.58" -> MapLocation("H0.58", "21346", Some("H0.58")),
		"S1.66" -> MapLocation("S1.66", "37944", Some("S1.66")),
		"S1.69" -> MapLocation("S1.69", "37916", Some("S1.69")),
		"H1.48" -> MapLocation("H1.48", "26971", Some("H1.48")),
		"B2.03 (Sci Conc)" -> MapLocation("B2.03", "31396", Some("B2.03 (Sci Conc)")),
		"B2.01 (Sci Conc)" -> MapLocation("B2.01", "31399", Some("B2.01 (Sci Conc)")),
		"LIB2" -> MapLocation("LIB2", "38872", Some("LIB2")),
		"S0.11" -> MapLocation("S0.11", "37478", Some("S0.11")),
		"H0.60" -> MapLocation("H0.60", "21347", Some("H0.60")),
		"H2.45" -> MapLocation("H2.45", "21538", Some("H2.45")),
		"S2.84" -> MapLocation("S2.84", "37731", Some("S2.84")),
		"S0.52" -> MapLocation("S0.52", "37419", Some("S0.52")),
		"H2.03" -> MapLocation("H2.03", "21518", Some("H2.03")),
		"EQ_C1.11/15" -> MapLocation("C1.11 / C1.13 / C1.1", "38082", Some("EQ_C1.11/15")),
		"B2.02 (Sci Conc)" -> MapLocation("B2.02", "31403", Some("B2.02 (Sci Conc)")),
		"WCE0.12" -> MapLocation("WCE0.12", "45808", Some("WCE0.12")),
		"W.MUSIC1" -> MapLocation("Music1", "20441", Some("W.MUSIC1")),
		"WCE0.9b" -> MapLocation("WCE0.09B", "45814", Some("WCE0.9b")),
		"H1.07" -> MapLocation("H1.07", "21426", Some("H1.07")),
		"L5" -> MapLocation("Lecture Theatre 5", "31389", Some("L5")),
		"LIB1" -> MapLocation("LIB1", "38890", Some("LIB1")),
		"H4.02" -> MapLocation("H4.02", "21659", Some("H4.02")),
		"S0.08" -> MapLocation("S0.08", "37492", Some("S0.08")),
		"W.MUSIC2" -> MapLocation("Music2", "20444", Some("W.MUSIC2")),
		"W.MUSIC3" -> MapLocation("Music3", "20447", Some("W.MUSIC3")),
		"CX_H2.04" -> MapLocation("H2.04", "21519", Some("CX_H2.04")),
		"CS_CS1.04" -> MapLocation("CS1.04", "26858", Some("CS_CS1.04")),
		"IB_0.013" -> MapLocation("0.013", "33994", Some("IB_0.013")),
		"WA0.15" -> MapLocation("WA0.15", "19139", Some("WA0.15")),
		"WA1.01" -> MapLocation("WA1.01", "19194", Some("WA1.01")),
		"PS1.28" -> MapLocation("PS1.28", "27644", Some("PS1.28")),
		"WA1.09" -> MapLocation("WA1.09", "19189", Some("WA1.09")),
		"WA1.15" -> MapLocation("WA1.15", "19198", Some("WA1.15")),
		"WCE0.9a" -> MapLocation("WCE0.09A", "45815", Some("WCE0.9a")),
		"R0.21" -> MapLocation("R0.21", "45730", Some("R0.21")),
		"R0.03/4" -> MapLocation("R0.03", "45735", Some("R0.03/4")),
		"R0.14" -> MapLocation("R0.14", "45705", Some("R0.14")),
		"R1.15" -> MapLocation("R1.15", "45291", Some("R1.15")),
		"R2.41" -> MapLocation("R2.41", "27745", Some("R2.41")),
		"R3.41" -> MapLocation("R3.41", "27786", Some("R3.41")),
		"R0.12" -> MapLocation("R0.12", "45706", Some("R0.12")),
		"R1.13" -> MapLocation("R1.13", "45270", Some("R1.13")),
		"R1.03" -> MapLocation("R1.03", "45294", Some("R1.03")),
		"R1.04" -> MapLocation("R1.04", "45278", Some("R1.04")),
		"S2.81" -> MapLocation("S2.81", "37697", Some("S2.81")),
		"H3.55" -> MapLocation("H3.55", "27029", Some("H3.55")),
		"R3.25" -> MapLocation("R3.25", "27762", Some("R3.25")),
		"LL_H0.64" -> MapLocation("H0.64", "21354", Some("LL_H0.64")),
		"LL_H0.61" -> MapLocation("H.061", "21345", Some("LL_H0.61")),
		"IP_R3.38" -> MapLocation("R3.38", "27783", Some("IP_R3.38")),
		"LL_H0.66" -> MapLocation("H0.66", "21356", Some("LL_H0.66")),
		"WCE0.10" -> MapLocation("WCE0.10", "45813", Some("WCE0.10")),
		"CS_CS1.01" -> MapLocation("CS1.01", "26849", Some("CS_CS1.01")),
		"CS_CS0.03" -> MapLocation("CS0.03", "26800", Some("CS_CS0.03")),
		"EN_H5.01" -> MapLocation("H5.01", "21760", Some("EN_H5.01")),
		"H4.01" -> MapLocation("H4.01", "21700", Some("H4.01")),
		"IN_R0.41 (PC room - Library)" -> MapLocation("L0.41", "38883", Some("IN_R0.41 (PC room - Library)")),
		"IN_R0.39 (PC room - Library)" -> MapLocation("L0.39", "38886", Some("IN_R0.39 (PC room - Library)")),
		"IN_S2.74 (PC room - Soc Sci)" -> MapLocation("S2.74", "37688", Some("IN_S2.74 (PC room - Soc Sci)")),
		"EC_S2.86" -> MapLocation("S2.86", "37733", Some("EC_S2.86")),
		"LF_ICLS" -> MapLocation("Computer room", "35380", Some("LF_ICLS")),
		"PS_H1.49a" -> MapLocation("H1.49A", "52919", Some("PS_H1.49a")),
		"MS.B3.03" -> MapLocation("MS.B3.03", "29094", Some("MS.B3.03")),
		"S0.50" -> MapLocation("S0.50", "37421", Some("S0.50")),
		"PX_CONCOURSE" -> MapLocation("Physics Concourse", "35565", Some("PX_CONCOURSE")), //TODO - no location ID set to P327 in the meantime
		"FI_A1.25" -> MapLocation("A1.25", "28587", Some("FI_A1.25")),
		"ST_C0.01" -> MapLocation("C0.01", "40876", Some("ST_C0.01")),
		"WM_1.04" -> MapLocation("IMC1.04", "46066", Some("WM_1.04")),
		"BS_BSR1" -> MapLocation("BSR1", "35398", Some("BS_BSR1")),
		"BS_BSR4" -> MapLocation("BSR4", "36984", Some("BS_BSR4")),
		"BS_BSR2" -> MapLocation("BSR2", "35399", Some("BS_BSR2")),
		"BS_BSR5" -> MapLocation("BSR5", "36985", Some("BS_BSR5")),
		"BS_LOCTUTS" -> MapLocation("BSR5", "36985", Some("BS_LOCTUTS")),
		"ST_A1.01" -> MapLocation("A1.01", "41203", Some("ST_A1.01")),
		"IB_M1" -> MapLocation("M1", "29367", Some("IB_M1")),
		"IN_A0.02 (PC room - Zeeman)" -> MapLocation("A0.02", "40811", Some("IN_A0.02 (PC room - Zeeman)")),
		"PX_P5.64" -> MapLocation("P5.64", "35446", Some("PX_P5.64")),
		"H4.22/4" -> MapLocation("H4.22/H4.24", "21669", Some("H4.22/4")),
		"H0.43" -> MapLocation("H0.43", "21402", Some("H0.43")),
		"H0.56" -> MapLocation("H0.56", "21339", Some("H0.56")),
		"MA_B3.01" -> MapLocation("B3.01", "29111", Some("MA_B3.01")),
		"OC0.03" -> MapLocation("OC0.03", "52117", Some("OC0.03")),
		"WA1.10" -> MapLocation("WA1.10", "19195", Some("WA1.10")),
		"WA1.20" -> MapLocation("WA1.20", "19213", Some("WA1.20")),
		"OC0.02" -> MapLocation("OC0.02", "52139", Some("OC0.02")),
		"A0.23 (Soc Sci)" -> MapLocation("A0.23", "37638", Some("A0.23 (Soc Sci)")),
		"H3.56" -> MapLocation("H3.56", "27030", Some("H3.56")),
		"H3.57" -> MapLocation("H3.57", "27031", Some("H3.57")),
		"H0.44" -> MapLocation("H0.44", "21327", Some("H0.44")),
		"CS_CS0.01" -> MapLocation("CS0.01", "26811", Some("CS_CS0.01")),
		"LA_S0.03" -> MapLocation("S0.03", "37496", Some("LA_S0.03")),
		"LA_S0.04" -> MapLocation("S0.04", "50947", Some("LA_S0.04")),
		"TH_G.56" -> MapLocation("Theatre Studies Rehearsal Room", "28405", Some("TH_G.56")),
		"TH_G.52" -> MapLocation("Rehearsal Room", "28425", Some("TH_G.52")),
		"TH_G.50" -> MapLocation("Centre for Cultural Policy Studies", "28426", Some("TH_G.50")),
		"TH_G.53" -> MapLocation("Studio 2", "28406", Some("TH_G.53")),
		"F.25A (Millburn)" -> MapLocation("F25a", "28613", Some("F.25A (Millburn)")),
		"HA_F37" -> MapLocation("F37", "28566", Some("HA_F37")),
		"SM_336" -> MapLocation("M3.36", "38558", Some("SM_336")),
		"EN_G.03" -> MapLocation("Writing Programme", "28377", Some("EN_G.03")),
		"TH_G.31" -> MapLocation("Video Editing Suite", "28490", Some("TH_G.31")),
		"OC0.04" -> MapLocation("OC0.04", "52144", Some("OC0.04")),
		"LA_S2.12" -> MapLocation("S2.12", "37776", Some("LA_S2.12")),
		"IN_B0.52 (PC room - G.Hill)" -> MapLocation("B0.52", "37017", Some("IN_B0.52 (PC room - G.Hill)")),
		"BS_E018" -> MapLocation("Teaching Laboratory 1", "35310", Some("BS_E018")),
		"BS_E109" -> MapLocation("Teaching Lab 3", "35375", Some("BS_E109")),
		"ES_F2.11" -> MapLocation("F2.11", "31029", Some("ES_F2.11")),
		"ES_F2.15" -> MapLocation("F2.15", "31019", Some("ES_F2.15")),
		"ES_D0.09" -> MapLocation("D0.09/D0.04", "30650", Some("ES_D0.09")),
		"H1.02" -> MapLocation("H1.02", "21431", Some("H1.02")),
		"CH_C5.06" -> MapLocation("C5.06", "44964", Some("CH_C5.06")),
		"CH_C5.21" -> MapLocation("Courtaulds Room", "44976", Some("CH_C5.21")),
		"ET_A0.14" -> MapLocation("A0.14 / A0.18", "37641", Some("ET_A0.14")),
		"ET_A1.05" -> MapLocation("A1.05", "38177", Some("ET_A1.05")),
		"ET_S1.71" -> MapLocation("S1.71", "37917", Some("ET_S1.71")),
		"ET_S2.85" -> MapLocation("S2.85", "37732", Some("ET_S2.85")),
		"ES_D2.02" -> MapLocation("D2.02", "51334", Some("ES_D2.02")),
		"ES_A4.01" -> MapLocation("A4.01", "31329", Some("ES_A4.01")),
		"MD_MTC0.04" -> MapLocation("C0.04", "36906", Some("MD_MTC0.04")),
		"MD_MTC0.05" -> MapLocation("C0.05", "37041", Some("MD_MTC0.05")),
		"MD_MTC0.06" -> MapLocation("MD0.06", "20712", Some("MD_MTC0.06")),
		"MD_MTC0.07" -> MapLocation("C0.07", "36909", Some("MD_MTC0.07")),
		"MD_MTC0.08" -> MapLocation("C0.08", "36911", Some("MD_MTC0.08")),
		"MD_MTC0.09" -> MapLocation("MD0.09", "20715", Some("MD_MTC0.09")),
		"MD_MTC0.10" -> MapLocation("C0.10", "36914", Some("MD_MTC0.10")),
		"MD_MTC0.11" -> MapLocation("C0.11", "37042", Some("MD_MTC0.11")),
		"MD_MTC1.08" -> MapLocation("C1.08", "37245", Some("MD_MTC1.08")),
		"MD_MTC1.10" -> MapLocation("C1.10", "37244", Some("MD_MTC1.10")),
		"MD_MTC1.11" -> MapLocation("C1.11", "37254", Some("MD_MTC1.11")),
		"MD_MTC1.09" -> MapLocation("C1.09", "37253", Some("MD_MTC1.09")),
		"GLT4" -> MapLocation("GLT4", "37016", Some("GLT4")),
		"CE_Pigeon Loft" -> MapLocation("Pigeon Loft", "20240", Some("CE_Pigeon Loft")),
		"IB_0.301" -> MapLocation("0.301", "33920", Some("IB_0.301")),
		"IB_1.301" -> MapLocation("1.301", "25466", Some("IB_1.301")),
		"CS_CS0.07" -> MapLocation("CS0.07", "26816", Some("CS_CS0.07")),
		"TH_G.55" -> MapLocation("Studio 1", "28410", Some("TH_G.55")),
		"IL_G.57" -> MapLocation("The CAPITAL Rehearsal Room", "28491", Some("IL_G.57")),
		"EN_G.08" -> MapLocation("Writers&#039; Room", "28378", Some("EN_G.08")),
		"ES_F0.25a" -> MapLocation("F0.25", "30679", Some("ES_F0.25a")),
		"ES_F0.25" -> MapLocation("F0.25", "30679", Some("ES_F0.25")),
		"PX_PS0.17a" -> MapLocation("PS0.17a", "27427", Some("PX_PS0.17a")),
		"LL_H0.78" -> MapLocation("H0.78", "21359", Some("LL_H0.78")),
		"LL_H0.67" -> MapLocation("H0.67", "21411", Some("LL_H0.67")),
		"IR_B0.41/43" -> MapLocation("B0.41/B0.43", "37605", Some("IR_B0.41/43")),
		"ST_C0.08" -> MapLocation("C0.08", "40871", Some("ST_C0.08")),
		"IB_1.003" -> MapLocation("1.003", "51379", Some("IB_1.003")),
		"IB_1.002b" -> MapLocation("1.002b", "51378", Some("IB_1.002b")),
		"IB_1.002a" -> MapLocation("1.002a", "51377", Some("IB_1.002a")),
		"IB_1.002" -> MapLocation("1.002 Postgraduate Learning Space", "51376", Some("IB_1.002")),
		"IB_0.004" -> MapLocation("0.004", "51478", Some("IB_0.004")),
		"IB_0.002a" -> MapLocation("0.002a", "51437", Some("IB_0.002a")),
		"IB_0.002" -> MapLocation("0.002 Undergraduate Learning Space", "51433", Some("IB_0.002")),
		"IB_1.007" -> MapLocation("1.007", "51423", Some("IB_1.007")),
		"IB_1.006" -> MapLocation("1.006", "51400", Some("IB_1.006")),
		"IB_1.005" -> MapLocation("1.005", "51401", Some("IB_1.005")),
		"IB_2.007" -> MapLocation("2.007", "51540", Some("IB_2.007")),
		"IB_SH8" -> MapLocation("Syndicate 8", "29303", Some("IB_SH8")),
		"IB_SH7" -> MapLocation("Syndicate 7", "29307", Some("IB_SH7")),
		"IB_SH6" -> MapLocation("Syndicate 6", "29305", Some("IB_SH6")),
		"IB_SH5" -> MapLocation("Syndicate 5", "29391", Some("IB_SH5")),
		"IB_SH4" -> MapLocation("Syndicate 4", "29306", Some("IB_SH4")),
		"IB_SH3" -> MapLocation("Syndicate 3", "29392", Some("IB_SH3")),
		"IB_SH2" -> MapLocation("Syndicate 2", "29333", Some("IB_SH2")),
		"IB_SH1" -> MapLocation("Syndicate 1", "29334", Some("IB_SH1")),
		"FAC.ART_H1.05" -> MapLocation("Hispanic Studies Office", "21428", Some("FAC.ART_H1.05")),
		"LL_H0.83" -> MapLocation("Language Centre Training Room", "21349", Some("LL_H0.83")),
		"WM_0.06" -> MapLocation("PIT Stop café", "52952", Some("WM_0.06")),
		"WM_1.10" -> MapLocation("IMC1.10", "46052", Some("WM_1.10")),
		"WM_1.09" -> MapLocation("IMC1.09", "46053", Some("WM_1.09")),
		"WM_1.08" -> MapLocation("IMC1.08", "46054", Some("WM_1.08")),
		"WM_2.23" -> MapLocation("IMC2.23", "19819", Some("WM_2.23")),
		"WM_2.21" -> MapLocation("IMC2.21", "19816", Some("WM_2.21")),
		"WM_2.19" -> MapLocation("IMC2.19", "19817", Some("WM_2.19")),
		"WM_1.15" -> MapLocation("IMC1.15", "46084", Some("WM_1.15")),
		"WM_1.14" -> MapLocation("IMC1.14", "46085", Some("WM_1.14")),
		"WM_1.12" -> MapLocation("IMC1.12", "46050", Some("WM_1.12")),
		"WM_1.11" -> MapLocation("IMC1.11", "46051", Some("WM_1.11")),
		"PX_P3.31" -> MapLocation("P331", "35570", Some("PX_P3.31")),
		"PX_P3.30" -> MapLocation("P330", "35577", Some("PX_P3.30")),
		"PX_P3.29" -> MapLocation("P329", "35564", Some("PX_P3.29")),
		"PX_P3.27" -> MapLocation("P327", "35565", Some("PX_P3.27")),
		"PX_P3.26" -> MapLocation("P3.26", "35572", Some("PX_P3.26")),
		"PX_P3.22" -> MapLocation("P3.22", "35586", Some("PX_P3.22")),
		"PX_P3.20" -> MapLocation("P3.20", "35573", Some("PX_P3.20")),
		"WM_2.50" -> MapLocation("IMC2.50", "19844", Some("WM_2.50")),
		"WM_2.49" -> MapLocation("IMC2.49", "19843", Some("WM_2.49")),
		"WM_2.46" -> MapLocation("IMC2.46", "19836", Some("WM_2.46")),
		"PX_PS0.16c" -> MapLocation("PS0.16C", "27436", Some("PX_PS0.16c")),
		"PX_P4.33" -> MapLocation("P4.33", "35502", Some("PX_P4.33")),
		"PX_P3.47" -> MapLocation("P3.47", "35589", Some("PX_P3.47")),
		"PX_P3.46" -> MapLocation("Physics Computer Suite", "35587", Some("PX_P3.46")),
		"PX_P3.36" -> MapLocation("P3.36", "35561", Some("PX_P3.36")),
		"PX_P3.32" -> MapLocation("Physics Dark Room", "35569", Some("PX_P3.32")),
		"LF_B0.07" -> MapLocation("BSR4", "36984", Some("LF_B0.07")),
		"LF_B0.02" -> MapLocation("BSR5", "36985", Some("LF_B0.02")),
		"BS_BSR3" -> MapLocation("BSR3", "37212", Some("BS_BSR3")),
		"LF_D1.37" -> MapLocation("BSR2", "35399", Some("LF_D1.37")),
		"ES_D1.02" -> MapLocation("D1.02", "30944", Some("ES_D1.02")),
		"ES_D1.01" -> MapLocation("D1.01", "30943", Some("ES_D1.01")),
		"LL_H0.82" -> MapLocation("H0.82", "21374", Some("LL_H0.82")),
		"LF_D1.41" -> MapLocation("BSR1", "35398", Some("LF_D1.41")),
		"MO_M3.01" -> MapLocation("M3.01", "38566", Some("MO_M3.01")),
		"WM_L0.09" -> MapLocation("L0.09", "47897", Some("WM_L0.09")),
		"WM_L0.10" -> MapLocation("L0.10", "47868", Some("WM_L0.10")),
		"WM_L0.12" -> MapLocation("L0.12", "47879", Some("WM_L0.12")),
		"WM_L0.11" -> MapLocation("L0.11", "47878", Some("WM_L0.11")),
		"WM_S0.12" -> MapLocation("S0.12", "47893", Some("WM_S0.12")),
		"WM_S0.14" -> MapLocation("S0.14", "47895", Some("WM_S0.14")),
		"WM_S0.13" -> MapLocation("S0.13", "47894", Some("WM_S0.13")),
		"WM_S0.11" -> MapLocation("S0.11", "47892", Some("WM_S0.11")),
		"WM_0.08" -> MapLocation("PIT Stop café", "52952", Some("WM_0.08")),
		"WM_S0.21" -> MapLocation("S0.21", "47875", Some("WM_S0.21")),
		"WM_S0.20" -> MapLocation("S0.20", "47902", Some("WM_S0.20")),
		"WM_S0.19" -> MapLocation("S0.19", "47901", Some("WM_S0.19")),
		"WM_S0.18" -> MapLocation("S0.18", "47900", Some("WM_S0.18")),
		"WM_S0.17" -> MapLocation("S0.17", "47865", Some("WM_S0.17")),
		"WM_S0.16" -> MapLocation("S0.16", "47864", Some("WM_S0.16")),
		"WM_S0.15" -> MapLocation("S0.15", "47896", Some("WM_S0.15")),
		"OC0.01" -> MapLocation("OC0.01", "52134", Some("OC0.01")),
		"OC1.01" -> MapLocation("OC1.01", "52205", Some("OC1.01")),
		"OC1.09" -> MapLocation("OC1.09", "52204", Some("OC1.09")),
		"OC1.04" -> MapLocation("OC1.04", "52175", Some("OC1.04")),
		"OC1.06" -> MapLocation("OC1.06", "52178", Some("OC1.06")),
		"OC0.05" -> MapLocation("OC0.05", "52131", Some("OC0.05")),
		"OC1.02" -> MapLocation("OC1.02", "52173", Some("OC1.02")),
		"OC1.03" -> MapLocation("OC1.03", "52174", Some("OC1.03")),
		"OC1.07" -> MapLocation("OC1.07", "52177", Some("OC1.07")),
		"OC1.08" -> MapLocation("OC1.08", "52176", Some("OC1.08")),
		"OC1.05" -> MapLocation("OC1.05", "52209", Some("OC1.05")),
		"MD_MSBA1.30" -> MapLocation("A1.30", "37149", Some("MD_MSBA1.30")),
		"MD_MSBA0.39" -> MapLocation("A0.39", "36944", Some("MD_MSBA0.39")),
		"ET_S1.102" -> MapLocation("S1.102", "37963", Some("ET_S1.102")),
		"ES_D0.04" -> MapLocation("D0.09/D0.04", "30650", Some("ES_D0.04")),
		"LL_H0.72" -> MapLocation("H0.72", "21357", Some("LL_H0.72")),

		// Additions for 17/18
		"W.AVON-DRAMA-STUDIO" -> MapLocation("Drama Studio", "19201", Some("W.AVON-DRAMA-STUDIO")),
		"LL_H0.79" -> MapLocation("H0.79", "21366", Some("LL_H0.79")),
		"WA0.24" -> MapLocation("WA0.24", "19135", Some("WA0.24")),
		"ES_A2.05a" -> MapLocation("A2.05A", "31009", Some("ES_A2.05a")),
		"ES_A2.05b" -> MapLocation("A2.05B", "31032", Some("ES_A2.05b")),

		// Renamed for 17/18 - (new owning department)
		"WT0.01" -> MapLocation("WT0.01", "38973", Some("WT0.01")), // was EP_WT0.01
		"WT0.06" -> MapLocation("WT0.06", "38964", Some("WT0.06")), // was EP_WT0.06 (lc)
		"WT1.03 (lab)" -> MapLocation("WT1.03", "38995", Some("WT1.03 (lab)")), // was EP_WT1.03 (lab)
		"WT1.06 (lab)" -> MapLocation("WT1.06", "38993", Some("WT1.06 (lab)")), // was EP_WT1.06 (lab)
		"LN_H4.43" -> MapLocation("H4.43", "21643", Some("LN_H4.43")), // was FR_H4.43
		"LN_H4.44" -> MapLocation("H4.44", "21642", Some("LN_H4.44")), // was FR_H4.44
		"LN_H2.02" -> MapLocation("H2.02", "21517", Some("LN_H2.02")), // was GE_H2.02
		"LN_H4.03" -> MapLocation("H4.03", "21658", Some("LN_H4.03")), // was IT_H4.03
		"IN_WCR (PC room - Westwood)" -> MapLocation("Multimedia Lab 1", "50593", Some("IN_WCR (PC room - Westwood)")), // was Multimedia Lab 1

		// Rooms not in use for 17/18 timetable but leaving them here anyway
		"LB_SEMINAR" -> MapLocation("L2.22", "39110", Some("LB_SEMINAR")),
		"LB_TEACHING" -> MapLocation("Experimental Teaching Space", "50520", Some("LB_TEACHING")),
		"WE0.01" -> MapLocation("WE0.01", "47650", Some("WE0.01")),
		"ARTS-BUTTERWORTH" -> MapLocation("Butterworth Hall", "42086", Some("ARTS-BUTTERWORTH")),
		"ES_D2.27" -> MapLocation("D2.27", "51329", Some("ES_D2.27")),
		"ES_RADCLIFFE" -> MapLocation("Radcliffe Conference Centre", "23895", Some("ES_RADCLIFFE")),
		"ES_SCARMAN" -> MapLocation("Scarman Conference Centre", "23880", Some("ES_SCARMAN")),
		"F1.07" -> MapLocation("F1.07", "30933", Some("F1.07")),
		"F1.10" -> MapLocation("F1.10", "30918", Some("F1.10")),
		"F1.11" -> MapLocation("F1.11", "30917", Some("F1.11")),
		"IB_3.007" -> MapLocation("3.007", "25685", Some("IB_3.007"))
	)
}
