package com.google.cloud.teleport.v2.templates;

import static com.google.cloud.teleport.v2.templates.utils.TestConstants.colFamily;
import static com.google.cloud.teleport.v2.templates.utils.TestConstants.colFamily2;
import static com.google.cloud.teleport.v2.templates.utils.TestConstants.colQualifier;
import static com.google.cloud.teleport.v2.templates.utils.TestConstants.colQualifier2;
import static com.google.cloud.teleport.v2.templates.utils.TestConstants.rowKey;
import static com.google.cloud.teleport.v2.templates.utils.TestConstants.rowKey2;
import static com.google.cloud.teleport.v2.templates.utils.TestConstants.timeT;
import static com.google.cloud.teleport.v2.templates.utils.TestConstants.value;
import static com.google.cloud.teleport.v2.templates.utils.TestConstants.value2;

import com.google.cloud.teleport.v2.templates.utils.HashUtils;
import com.google.cloud.teleport.v2.templates.utils.HbaseUtils;
import com.google.cloud.teleport.v2.templates.utils.RowMutationsCoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import org.apache.hadoop.hbase.client.RowMutations;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test that {@link RowMutationsCoder} encoding does not change {@link RowMutations}
 * object.
 */
@RunWith(JUnit4.class)
public class RowMutationsCoderTest {
  private static final Logger log = LoggerFactory.getLogger(RowMutationsCoderTest.class);

  private final RowMutationsCoder coder = RowMutationsCoder.of();
  private ByteArrayOutputStream outputStream;
  private ByteArrayInputStream inputStream;

  /**
   * Asserts two {@link RowMutations} objects are equal.
   * @param rowMutationA
   * @param rowMutationB
   * @throws Exception if hash function fails
   */
  private void assertRowMutationsEqual(RowMutations rowMutationA, RowMutations rowMutationB) throws Exception {
    Assert.assertTrue(Arrays.equals(rowMutationA.getRow(), rowMutationB.getRow()));
    Assert.assertEquals(
        HashUtils.hashMutationList(rowMutationA.getMutations()),
        HashUtils.hashMutationList(rowMutationB.getMutations())
    );
  }

  @Before
  public void setUp() {
    outputStream = new ByteArrayOutputStream();
  }
  @After
  public void tearDown() throws IOException {
    outputStream.close();
  }

  @Test
  public void encodePut() throws Exception {
    RowMutations put =
        new RowMutations(rowKey.getBytes())
            .add(
                Arrays.asList(
                    HbaseUtils.HbaseMutationBuilder.createPut(rowKey, colFamily, colQualifier, value, timeT)));
    coder.encode(put, outputStream);

    inputStream = new ByteArrayInputStream(outputStream.toByteArray());

    RowMutations decodedPut = coder.decode(inputStream);

    Assert.assertTrue(inputStream.readAllBytes().length == 0);
    assertRowMutationsEqual(put, decodedPut);
  }

  @Test
  public void encodeMultipleMutations() throws Exception {
    RowMutations multipleMutations  =
        new RowMutations(rowKey.getBytes())
            .add(
                Arrays.asList(
                    HbaseUtils.HbaseMutationBuilder.createPut(rowKey, colFamily, colQualifier, value, timeT),
                    HbaseUtils.HbaseMutationBuilder.createDelete(rowKey, colFamily, colQualifier, timeT),
                    HbaseUtils.HbaseMutationBuilder.createDeleteFamily(rowKey, colFamily, timeT)));
    coder.encode(multipleMutations, outputStream);

    inputStream = new ByteArrayInputStream(outputStream.toByteArray());

    RowMutations decodedMultipleMutations = coder.decode(inputStream);

    Assert.assertTrue(inputStream.available() == 0);
    assertRowMutationsEqual(multipleMutations, decodedMultipleMutations);
  }

  @Test
  public void encodeMultipleRowMutations() throws Exception {
    RowMutations put =
        new RowMutations(rowKey.getBytes())
            .add(
                Arrays.asList(
                    HbaseUtils.HbaseMutationBuilder.createPut(rowKey, colFamily, colQualifier, value, timeT)));
    RowMutations deleteCols =
        new RowMutations(rowKey.getBytes())
            .add(
                Arrays.asList(
                    HbaseUtils.HbaseMutationBuilder.createDelete(rowKey, colFamily, colQualifier, timeT)));
    RowMutations deleteFamily =
        new RowMutations(rowKey.getBytes())
            .add(
                Arrays.asList(
                    HbaseUtils.HbaseMutationBuilder.createDeleteFamily(rowKey, colFamily, timeT)));

    coder.encode(put, outputStream);
    coder.encode(deleteCols, outputStream);
    coder.encode(deleteFamily, outputStream);

    inputStream = new ByteArrayInputStream(outputStream.toByteArray());

    RowMutations decodedPut = coder.decode(inputStream);
    RowMutations decodedDeleteCols = coder.decode(inputStream);
    RowMutations decodedDeleteFamily = coder.decode(inputStream);

    Assert.assertTrue(inputStream.available() == 0);
    assertRowMutationsEqual(put, decodedPut);
    assertRowMutationsEqual(deleteCols, decodedDeleteCols);
    assertRowMutationsEqual(deleteFamily, decodedDeleteFamily);
  }

  @Test
  public void encodeMultipleComplexRowMutations() throws Exception {
    RowMutations complexMutation =
        new RowMutations(rowKey.getBytes())
            .add(
                Arrays.asList(
                    HbaseUtils.HbaseMutationBuilder.createPut(rowKey, colFamily, colQualifier, value, timeT),
                    HbaseUtils.HbaseMutationBuilder.createDelete(rowKey, colFamily2, colQualifier2, timeT+1),
                    HbaseUtils.HbaseMutationBuilder.createDeleteFamily(rowKey, colFamily, timeT)));

    coder.encode(complexMutation, outputStream);
    coder.encode(complexMutation, outputStream);
    coder.encode(complexMutation, outputStream);

    inputStream = new ByteArrayInputStream(outputStream.toByteArray());

    RowMutations decodedComplexMutation = coder.decode(inputStream);
    RowMutations decodedComplexMutation2 = coder.decode(inputStream);
    RowMutations decodedComplexMutation3 = coder.decode(inputStream);

    Assert.assertTrue(inputStream.available() == 0);
    assertRowMutationsEqual(complexMutation, decodedComplexMutation);
    assertRowMutationsEqual(complexMutation, decodedComplexMutation2);
    assertRowMutationsEqual(complexMutation, decodedComplexMutation3);
  }

}