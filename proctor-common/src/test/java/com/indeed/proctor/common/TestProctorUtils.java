package com.indeed.proctor.common;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author parker
 */
public class TestProctorUtils {

    private static final String TEST_A = "testA";
    private static final String TEST_B = "testB";


    @Test
    public void verifyAndConsolidateShouldTestAllocationSum() throws IncompatibleTestMatrixException {
        List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
        Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets));
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets,
                                                      // Allocations do not add up to 1
                                                      fromCompactAllocationFormat("ruleA|-1:0.5,0:0.5,1:0.0", "-1:0.0,0:0.0,1:0.0")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // verifyAndConsolidate should not throw an error because the 'invalidbuckets' test is not required.
            assertEquals(1, matrix.getTests().size());
            assertValid("invalid test not required, sum{allocations} < 1.0", matrix, Collections.<String, TestSpecification>emptyMap());
            assertEquals("non-required tests should be removed from the matrix", 0, matrix.getTests().size());
        }
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets,
                                                      // Allocations do not add up to 1
                                                      fromCompactAllocationFormat("ruleA|-1:0.5,0:0.5,1:0.0", "-1:0.0,0:0.0,1:0.0")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // verifyAndConsolidate should throw an error because the 'invalidbuckets' test is required.
            assertEquals(1, matrix.getTests().size());
            assertInvalid("bucket allocation sums are unchecked, sum{allocations} < 1.0", matrix, requiredTests);
            assertEquals("required tests should not be removed from the matrix", 1, matrix.getTests().size());
        }
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets,
                                                      // Allocations do not add up to 1
                                                      fromCompactAllocationFormat("ruleA|-1:0.5,0:0.5,1:0.0", "-1:0.5,0:0.5,1:0.5")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertEquals(1, matrix.getTests().size());
            // verifyAndConsolidate should not throw an error because the 'invalidbuckets' test is not required.
            assertValid("invalid test not required, sum{allocations} > 1.0", matrix, Collections.<String, TestSpecification>emptyMap());
            assertEquals("non-required tests should be removed from the matrix", 0, matrix.getTests().size());
        }
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets,
                                                      // Allocations do not add up to 1
                                                      fromCompactAllocationFormat("ruleA|-1:0.5,0:0.5,1:0.0", "-1:0.5,0:0.5,1:0.5")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertEquals(1, matrix.getTests().size());
            // verifyAndConsolidate should throw an error because the 'testa' test is required.
            assertInvalid("bucket allocation sums are unchecked, sum{allocations} > 1.0", matrix, requiredTests);
            assertEquals("required tests should not be removed from the matrix", 1, matrix.getTests().size());
        }
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets,
                                                      // Allocations add up to 1.0
                                                      fromCompactAllocationFormat("ruleA|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertEquals(1, matrix.getTests().size());
            assertValid("bucket allocation sums are unchecked, sum{allocations} == 1.0", matrix, Collections.<String, TestSpecification>emptyMap());
            assertEquals("non-required tests should be removed from the matrix", 0, matrix.getTests().size());
        }
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets,
                                                      // Allocations add up to 1.0
                                                      fromCompactAllocationFormat("ruleA|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertEquals(1, matrix.getTests().size());
            assertValid("bucket allocation sums are unchecked, sum{allocations} == 1.0", matrix, requiredTests);
            assertEquals("required tests should not be removed from the matrix", 1, matrix.getTests().size());
        }
    }

    @Test
    public void verifyAndConsolidateShouldFailIfMissingDefaultAllocation() throws IncompatibleTestMatrixException {
        List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
        Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets));
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets,
                                                      // Allocations all have rules
                                                      fromCompactAllocationFormat("ruleA|-1:0.0,0:0.0,1:1.0", "ruleB|-1:0.5,0:0.5,1:0.0")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertValid("test missing empty rule is not required", matrix, Collections.<String, TestSpecification>emptyMap());
            assertMissing("test missing empty rule is required", matrix, requiredTests);
        }
    }

    @Test
    public void verifyAndConsolidateShouldFailIfNoAllocations() throws IncompatibleTestMatrixException {
        List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");

        Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets));
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets, Collections.<Allocation>emptyList()));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertValid("test missing allocations is not required", matrix, Collections.<String, TestSpecification>emptyMap());
        }
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets, Collections.<Allocation>emptyList()));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertInvalid("test missing allocations is required", matrix, requiredTests);
        }
    }

    @Test
    public void unknownBucketWithAllocationGreaterThanZero() throws IncompatibleTestMatrixException {
        // The test-matrix has 3 buckets
        List<TestBucket> buckets = fromCompactBucketFormat("zero:0,one:1,two:2");
        // The proctor-specification only knows about two of the buckets
        final TestSpecification testSpecification = transformTestBuckets(fromCompactBucketFormat("zero:0,one:1"));
        Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, testSpecification);

        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            // Allocation of bucketValue=2 is > 0
            final ConsumableTestDefinition testDefinition = constructDefinition(buckets, fromCompactAllocationFormat("0:0,1:0,2:1.0"));
            tests.put(TEST_A, testDefinition);

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // externally inconsistent matrix
            assertInvalid("allocation for externally unknown bucket (two) > 0", matrix, requiredTests);
            assertEquals("trivially expected only one test in the matrix", 1, matrix.getTests().size());
            final List<Allocation> allocations = matrix.getTests().values().iterator().next().getAllocations();
            assertEquals("trivially expected only one allocation in the test (same as source)", 1, allocations.size());
            final List<Range> ranges = allocations.iterator().next().getRanges();
            assertEquals("Expected the ranges to be reduced from 3 to 1, since only the fallback value is now present", 1, ranges.size());
            final Range onlyRange = ranges.iterator().next();
            assertEquals("Should have adopted the fallback value from the test spec", onlyRange.getBucketValue(), testSpecification.getFallbackValue());
            assertEquals("Trivially should have been set to 100% fallback", 1.0, onlyRange.getLength(), 0.005);
        }
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            // Allocation of bucketValue=2 is == 0
            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("0:0.5,1:0.5,2:0")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertValid("allocation for externally unknown bucket (two) == 0", matrix, requiredTests);
        }
    }

    @Test
    public void internallyUnknownBucketWithAllocationGreaterThanZero() throws IncompatibleTestMatrixException {
         // The test-matrix has 3 buckets
        List<TestBucket> buckets = fromCompactBucketFormat("zero:0,one:1,two:2");
        Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets));

        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            // Allocation has 4 buckets, bucket with non-zero allocation in an unknown bucket
            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("0:0,1:0,2:0.5,3:0.5")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally inconsistent matrix
            assertInvalid("allocation for internally unknown bucket (three) > 0", matrix, requiredTests);
        }
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            // There is an unknown bucket with non-zero allocation
            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("0:0,1:0,2:0.5,3:0.5")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally inconsistent matrix
            assertInvalid("allocation for internally unknown bucket (three) == 0", matrix, requiredTests);
        }
    }

    @Test
    public void requiredTestBucketsMissing() throws IncompatibleTestMatrixException {
         // The test-matrix has fewer buckets than the required tests
        List<TestBucket> buckets_matrix = fromCompactBucketFormat("zero:0,one:1");
        List<TestBucket> buckets_required = fromCompactBucketFormat("zero:0,one:1,two:2,three:3");
        Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets_required));

        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets_matrix, fromCompactAllocationFormat("0:0,1:1.0")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally inconsistent matrix
            assertValid("test-matrix has a subset of required buckets", matrix, requiredTests);
        }
    }

    @Test
    public void bucketsNameAndValuesShouldBeConsistent() throws IncompatibleTestMatrixException {
        {
            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(fromCompactBucketFormat("zero:0,one:1")));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            // The Bucket Names and Values intentionally do not match
            tests.put(TEST_A, constructDefinition(fromCompactBucketFormat("zero:1,one:0"),
                                                  fromCompactAllocationFormat("0:0,1:1.0")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertValid("test-matrix has a different {bucketValue} -> {bucketName} mapping than required Tests", matrix, requiredTests);
        }
        {
            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(fromCompactBucketFormat("zero:0,one:1,two:2")));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            // The Bucket Names and Values intentionally do not match
            tests.put(TEST_A, constructDefinition(fromCompactBucketFormat("zero:0,one:2"),
                                                  fromCompactAllocationFormat("0:0,2:1.0")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertValid("test-matrix has a different {bucketValue} -> {bucketName} mapping than required Tests", matrix, requiredTests);
        }
    }

    @Test
    public void requiredTestIsMissing() throws IncompatibleTestMatrixException {
         // The test-matrix has 3 buckets
        List<TestBucket> buckets_A = fromCompactBucketFormat("zero:0,one:1,two:2");
        final TestSpecification testSpecA = transformTestBuckets(buckets_A);

        List<TestBucket> buckets_B = fromCompactBucketFormat("foo:0,bar:1");
        final TestSpecification testSpecB = transformTestBuckets(buckets_B);
        testSpecB.setFallbackValue(-2); // unusual value;

        Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, testSpecA, TEST_B, testSpecB);

        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets_A, fromCompactAllocationFormat("0:0,1:0.5,2:0.5")));

            // Artifact only has 1 of the 2 required tests
            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally inconsistent matrix
            assertNull("missing testB should not be present prior to consolidation", matrix.getTests().get(TEST_B));
            assertMissing("required testB is missing from the test matrix", matrix, requiredTests);

            final ConsumableTestDefinition consolidatedTestB = matrix.getTests().get(TEST_B);
            assertNotNull("autogenerated testB definition missing from consolidated matrix", consolidatedTestB);
            assertEquals(
                    "autogenerated testB definition should have used custom fallback value",
                    testSpecB.getFallbackValue(),
                    consolidatedTestB.getAllocations().get(0).getRanges().get(0).getBucketValue());

        }
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets_A, fromCompactAllocationFormat("0:0,1:0.5,2:0.5")));
            tests.put(TEST_B, constructDefinition(buckets_B, fromCompactAllocationFormat("0:0.5,1:0.5")));

            // Artifact both of the required tests
            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("both required tests are present in the matrix", matrix, requiredTests);
        }
        {
            Map<String, TestSpecification> only_TestA_Required = ImmutableMap.of(TEST_A, transformTestBuckets(buckets_A));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets_A, fromCompactAllocationFormat("0:0,1:0.5,2:0.5")));
            // Intentionally making the non-required test B allocation sum to 0.5
            tests.put(TEST_B, constructDefinition(buckets_B, fromCompactAllocationFormat("0:0,1:0.5")));

            // Artifact both of the required tests
            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertEquals(2, matrix.getTests().size());
            assertValid("required test A is present in the matrix", matrix, only_TestA_Required);

            assertEquals("Only required test A should remain in the matrix", 1, matrix.getTests().size());
            assertTrue("Only required test A should remain in the matrix", matrix.getTests().containsKey(TEST_A));
            assertFalse("Only required test A should remain in the matrix", matrix.getTests().containsKey(TEST_B));
        }
    }

    @Test
    public void verifyBucketPayloads() throws IncompatibleTestMatrixException {
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setLongValue(-1L);
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setLongValue(1L);
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setLongValue(1L);
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "longValue", null));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("all payloads of the same type", matrix, requiredTests);
        }
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setLongValue(-1L);
            buckets.get(0).setPayload(p);
            // bucket 1 is missing a payload here.
            p = new Payload();
            p.setLongValue(1L);
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "longValue", null));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid("not all payloads of the test defined", matrix, requiredTests);
        }
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setStringValue("inact");
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setStringValue("foo");
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setStringValue("bar");
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "longValue", null));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid("all payloads of the wrong type", matrix, requiredTests);
        }
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setLongValue(-1L);
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setLongValue(0L);
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setStringValue("foo");
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "longValue", null));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally inconsistent matrix: different payload types in test
            assertInvalid("all payloads not of the same type", matrix, requiredTests);
        }
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setStringArray(new String[]{});  // empty arrays are allowed.
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setStringArray(new String[]{"foo", "bar"});
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setStringArray(new String[]{"baz", "quux", "xyzzy"});
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "stringArray", null));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("vector payloads can be different lengths", matrix, requiredTests);
        }
    }

    @Test
    public void verifyBucketPayloadValueValidators() throws IncompatibleTestMatrixException {
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setDoubleValue(0D);
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setDoubleValue(10D);
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setDoubleValue(20D);
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "doubleValue", "${value >= 0}"));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("doubleValue: all payload values pass validation", matrix, requiredTests);
        }
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setDoubleValue(0D);
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setDoubleValue(10D);
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setDoubleValue(-1D);
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "doubleValue", "${value >= 0}"));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid("doubleValue: a payload value doesn't pass validation", matrix, requiredTests);
        }
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setLongValue(0L);
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setLongValue(10L);
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setLongValue(20L);
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "longValue", "${value >= 0}"));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("longValue: all payload values pass validation", matrix, requiredTests);
        }
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setLongValue(0L);
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setLongValue(10L);
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setLongValue(-1L);
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "longValue", "${value >= 0}"));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid("longValue: a payload value doesn't pass validation", matrix, requiredTests);
        }
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setStringValue("inactive");
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setStringValue("foo");
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setStringValue("bar");
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "stringValue", "${value >= \"b\"}"));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("stringValue: all payload values pass validation", matrix, requiredTests);
        }
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setStringValue("inactive");
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setStringValue("foo");
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setStringValue("abba");
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "stringValue", "${value >= \"b\"}"));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid("stringValue: a payload value doesn't pass validation", matrix, requiredTests);
        }
    }

    public void verifyBucketPayloadArrayValidators() throws IncompatibleTestMatrixException {
        // TODO(pwp): add
    }

    @Test
    public void verifyPayloadDeploymentScenerios() throws IncompatibleTestMatrixException {
        {
            // Proctor should not break if it consumes a test matrix
            // that has a payload even if it's not expecting one.
            // (So long as all the payloads are of the same type.)
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setLongValue(-1L);
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setLongValue(0L);
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setLongValue(1L);
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets)); // no payload type specified
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("no payload expected; payloads supplied of the same type", matrix, requiredTests);

            // Should have gotten back a test.
            assertEquals("required tests should not be removed from the matrix", 1, matrix.getTests().size());

            // Make sure we don't have any payloads in the resulting tests.
            for (Entry<String, ConsumableTestDefinition> next : matrix.getTests().entrySet()) {
                final ConsumableTestDefinition testDefinition = next.getValue();

                for (final TestBucket bucket : testDefinition.getBuckets()) {
                    assertNull(bucket.getPayload());
                }
            }
        }
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setLongValue(-1L);
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setLongValue(0L);
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setStringValue("foo");
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets)); // no payload type specified
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally inconsistent matrix: different payload types in test
            assertInvalid("no payload expected; payloads supplied of different types", matrix, requiredTests);
        }
        {
            // Proctor should not break if it consumes a test matrix
            // with no payloads when it is expecting one.
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "longValue", null));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("payload expected; no payloads supplied", matrix, requiredTests);
            // Should have gotten back a test.
            assertEquals("required tests should not be removed from the matrix", 1, matrix.getTests().size());
        }
    }

    @Test
    public void testCompactAllocationFormat() {
//        List<Allocation> allocations_empty = fromCompactAllocationFormat("");
//        assertEquals(0, allocations_empty.size());
        double DELTA = 0;

        List<Allocation> allocations = fromCompactAllocationFormat("ruleA|10:0,11:0.5,12:0.5", "0:0,1:0.5,2:0.5");
        assertEquals(2, allocations.size());
        {
            Allocation allocationA = allocations.get(0);
            assertEquals("ruleA", allocationA.getRule());
            assertEquals(3, allocationA.getRanges().size());
            assertEquals(10, allocationA.getRanges().get(0).getBucketValue());
            assertEquals(0, allocationA.getRanges().get(0).getLength(), DELTA);
            assertEquals(11, allocationA.getRanges().get(1).getBucketValue());
            assertEquals(0.5, allocationA.getRanges().get(1).getLength(), DELTA);
            assertEquals(12, allocationA.getRanges().get(2).getBucketValue());
            assertEquals(0.5, allocationA.getRanges().get(2).getLength(), DELTA);
        }
        {
            Allocation allocationB = allocations.get(1);
            assertNull(allocationB.getRule());
            assertEquals(3, allocationB.getRanges().size());
            assertEquals(0, allocationB.getRanges().get(0).getBucketValue());
            assertEquals(0, allocationB.getRanges().get(0).getLength(), DELTA);
            assertEquals(1, allocationB.getRanges().get(1).getBucketValue());
            assertEquals(0.5, allocationB.getRanges().get(1).getLength(), DELTA);
            assertEquals(2, allocationB.getRanges().get(2).getBucketValue());
            assertEquals(0.5, allocationB.getRanges().get(2).getLength(), DELTA);
        }



    }
    @Test
    public void testCompactBucketFormatHelperMethods() {
//        List<TestBucket> buckets_empty = fromCompactBucketFormat("");
//        assertEquals(0, buckets_empty.size());
        List<TestBucket> buckets = fromCompactBucketFormat("zero:0,one:1,two:2");

        assertEquals(3, buckets.size());
        assertEquals("zero", buckets.get(0).getName());
        assertEquals(0, buckets.get(0).getValue());
        assertEquals("one", buckets.get(1).getName());
        assertEquals(1, buckets.get(1).getValue());
        assertEquals("two", buckets.get(2).getName());
        assertEquals(2, buckets.get(2).getValue());
    }

    /* Test Helper Methods Below */

    private void assertInvalid(String msg, TestMatrixArtifact matrix, Map<String, TestSpecification> requiredTests) throws IncompatibleTestMatrixException {
        assertErrorCreated(false, true, msg, matrix, requiredTests);
    }
    private void assertMissing(String msg, TestMatrixArtifact matrix, Map<String, TestSpecification> requiredTests) throws IncompatibleTestMatrixException {
        assertErrorCreated(true, false, msg, matrix, requiredTests);
    }
    private void assertValid(String msg, TestMatrixArtifact matrix, Map<String, TestSpecification> requiredTests) throws IncompatibleTestMatrixException {
        assertErrorCreated(false, false, msg, matrix, requiredTests);
    }
    private void assertErrorCreated(boolean hasMissing, boolean hasInvalid, String msg, TestMatrixArtifact matrix, Map<String, TestSpecification> requiredTests) throws IncompatibleTestMatrixException {
        final ProctorLoadResult proctorLoadResult = ProctorUtils.verifyAndConsolidate(matrix, "[ testcase: " + msg + " ]", requiredTests, RuleEvaluator.FUNCTION_MAPPER);

        final Set<String> missingTests = proctorLoadResult.getMissingTests();
        assertEquals(msg, hasMissing, !missingTests.isEmpty());

        final Set<String> testsWithErrors = proctorLoadResult.getTestsWithErrors();
        assertEquals(msg, hasInvalid, !testsWithErrors.isEmpty());
    }


    private TestMatrixArtifact constructArtifact(Map<String, ConsumableTestDefinition> tests) {
        final TestMatrixArtifact matrix = new TestMatrixArtifact();

        matrix.setAudit(constructAudit());

        matrix.setTests(tests);
        return matrix;
    }

    private Audit constructAudit() {
        final Audit audit = new Audit();
        audit.setVersion(1);
        audit.setUpdatedBy("unit test");
        audit.setUpdated(1337133701337L);
        return audit;
    }

    private ConsumableTestDefinition constructDefinition(List<TestBucket> buckets,
                                                         List<Allocation> allocations) {

        final ConsumableTestDefinition test = new ConsumableTestDefinition();
        test.setVersion(0); // don't care about version for this test
        test.setSalt(null); // don't care about salt for this test
        test.setRule(null); // don't care about rule for this test
        test.setConstants(Collections.<String, Object>emptyMap()); // don't care about constants for this test

        test.setBuckets(buckets);
        test.setAllocations(allocations);
        return test;
    }

    /*  *********************************************************************
        The compact format is used because it's easier to quickly list bucket
        allocations and bucket values than to use string JSON
     *  ********************************************************************* */

    private List<Allocation> fromCompactAllocationFormat(String ... allocations) {
        final List<String> allocationList = Lists.newArrayListWithExpectedSize(allocations.length);
        for(String s : allocations) {
            allocationList.add(s);
        }
        return fromCompactAllocationFormat(allocationList);
    }
    private List<Allocation> fromCompactAllocationFormat(List<String> allocations) {
        final List<Allocation> allocationList = Lists.newArrayListWithExpectedSize(allocations.size());
        // rule|0:0,0:.0.1,0:.2
        for(String allocation : allocations) {
            final String[] parts = allocation.split("\\|");
            final String rule;
            final String sRanges;
            if(parts.length == 1) {
                rule = null;
                sRanges = parts[0];
            } else if (parts.length == 2) {
                rule = parts[0];
                sRanges = parts[1];
            } else {
                System.out.println("parts : " + parts.length);
                throw new IllegalArgumentException("Invalid compact allocation format [" + allocation + "], expected: rule|<bucketValue>:<length>, ...<bucketValue-N>:<length-N>.");
            }
            String[] allRanges = sRanges.split(",");
            final List<Range> ranges = Lists.newArrayListWithCapacity(allRanges.length);
            for(String sRange : allRanges) {
                // Could handle index-out of bounds + number formatting exception better.
                String[] rangeParts = sRange.split(":");
                ranges.add(new Range(Integer.parseInt(rangeParts[0], 10), Double.parseDouble(rangeParts[1])));
            }
            allocationList.add(new Allocation(rule, ranges));
        }
        return allocationList;
    }

    private List<TestBucket> fromCompactBucketFormat(String sBuckets){
        String[] bucketParts = sBuckets.split(",");
        List<TestBucket> buckets = Lists.newArrayListWithCapacity(bucketParts.length);
        for(int i = 0; i < bucketParts.length; i++) {
            // Could handle index-out of bounds + number formatting exception better.
            final String[] nameAndValue = bucketParts[i].split(":");
            buckets.add(new TestBucket(nameAndValue[0], Integer.parseInt(nameAndValue[1]), "bucket " + i, null));
        }
        return buckets;
    }

    private TestSpecification transformTestBuckets(List<TestBucket> testBuckets) {
        TestSpecification testSpec = new TestSpecification();
        Map<String, Integer> buckets = Maps.newLinkedHashMap();
        for(TestBucket b : testBuckets) {
            buckets.put(b.getName(), b.getValue());
        }
        testSpec.setBuckets(buckets);
        return testSpec;
    }

    private TestSpecification transformTestBuckets(List<TestBucket> testBuckets, String payloadType, String validator) {
        TestSpecification testSpec = transformTestBuckets(testBuckets);
        PayloadSpecification payloadSpec = new PayloadSpecification();
        payloadSpec.setType(payloadType);
        payloadSpec.setValidator(validator);
        testSpec.setPayload(payloadSpec);
        return testSpec;
    }


    //
}
