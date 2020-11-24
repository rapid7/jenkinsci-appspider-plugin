package com.rapid7.appspider.datatransferobjects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.json.JSONException;

@ExtendWith(MockitoExtension.class)
class ScanResultTests {
    
    @Mock
    private JSONObject jsonObject;

    @Test
    void ctor_throwsIllegalArgumentException_whenJsonObjectIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new ScanResult((JSONObject) null));
    }

    @Test
    void ctor_throwsIllegalArgumentException_whenJsonObjectThrowsJSONException() {
        when(jsonObject.getBoolean(any(String.class))).thenThrow(new JSONException("test"));
        assertThrows(IllegalArgumentException.class, () -> new ScanResult(jsonObject));
    }

    @Test
    void ctor_throwsIllegalArgumentException_whenReturnsEmptyObject() {
        when(jsonObject.getJSONObject("Scan")).thenReturn(new JSONObject());
        assertThrows(IllegalArgumentException.class, () -> new ScanResult(jsonObject));
    }

    @Test
    void getScanId_returnsGivenScanId() {
        ScanResult result = new ScanResult(true, "id");
        assertEquals("id", result.getScanId());
    }

    @Test
    void isSuccess_returnsGivenIsSuccess() {
        ScanResult result = new ScanResult(true, "id");
        assertEquals(true, result.isSuccess());
    }

    @Test
    void getScanId_returnsGivenScanIdInJSON() {
        when(jsonObject.getJSONObject("Scan")).thenReturn(jsonObject);
        when(jsonObject.getString("Id")).thenReturn("id");

        ScanResult result = new ScanResult(jsonObject);

        assertEquals("id", result.getScanId());
    }

    @Test
    void isSuccess_returnsGivenIsSuccessInJSON() {
        when(jsonObject.getJSONObject("Scan")).thenReturn(jsonObject);
        when(jsonObject.getString("Id")).thenReturn("id");
        when(jsonObject.getBoolean("IsSuccess")).thenReturn(true);

        ScanResult result = new ScanResult(jsonObject);

        assertEquals(true, result.isSuccess());
    }
}
